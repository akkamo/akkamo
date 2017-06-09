package eu.akkamo.web

import akka.http.scaladsl.server.Route
import com.typesafe.config.{Config, ConfigFactory}
import eu.akkamo.m.config.Transformer
import eu.akkamo.web.WebContentRegistry.{ContentMapping, RouteGenerator}
import eu.akkamo.{Context, ContextError, Initializable, LoggingAdapterFactory, Module, Publisher, Registry, RouteRegistry, Runnable, TypeInfoChain}

import scala.util.Try

/**
  * Represents registry for single configured static content mapping.
  */
class WebContentRegistry(
                          val routeRegistryAlias: Option[String],
                          val mapping: Map[String, RouteGenerator]) extends Registry[ContentMapping] {

  override def copyWith(p: (String, RouteGenerator)): WebContentRegistry.this.type = {
    if (mapping.contains(p._1)) throw ContextError(s"A RouteGenerator under alias: ${p._1} is already registered")
    else new WebContentRegistry(routeRegistryAlias, mapping = mapping + p).asInstanceOf[this.type]
  }
}

object WebContentRegistry {
  type RouteGenerator = () => Route
  type ContentMapping = (String, RouteGenerator)
}

/**
  * ''Akkamo module'' allowing to serve static content either from the classpath resource or
  * selected directory. The ways how to serve the static content can be extended at runtime by
  * plugging in custom implementation of [[WebContentRegistry]], or implementing custom
  * [[WebContentRegistry.RouteGenerator]] (see `routeGenerators` section in example documentation
  * below or [[FileFromDirGenerator]] example implementation).
  *
  * = Configuration example =
  * {{{
  *   akkamo.webContent = {
  *     // empty WebContentRegistryCreated
  *     // RouteGenerators must be added manualy
  *     name1 = {
  *       routeRegistryAlias = "akkaHttpAlias"
  *       aliases = ["alias1", "alias2"]
  *     },
  *     // WebContentRegistryCreated containing one
  *     // RouteGenerator serving content of web directory and listening on .../web
  *     name2 = {
  *       default = true // at least in one configuration is value mandatory if the number of instances is > 1
  *       routeGenerators = {
  *         web = {
  *          class = eu.akkamo.web.FileFromDirGenerator
  *          parameters = ["/web"]
  *         }
  *       }
  *     }
  *   }
  * }}}
  *
  * If no configuration is provided, default configuration equivalent to configuration snipped
  * below is used, trying to serve the static content from `/web` directory, either found at
  * classpath or file system (see [[FileFromDirGenerator]] for more details):
  * If there is not "/web" directory/resource on path, then routeGenerators section is empty
  *
  * {{{
  * akkamo.webContent = {
  *   default = {
  *     routeRegistryAlias="akkamo_webContent"
  *     routeGenerators = {
  *      web = {
  *         class = eu.akkamo.web.FileFromDirGenerator
  *         parameters = ["web"] // directory from data going to be served
  *       }
  *     }
  *   }
  * }
  * }}}
  *
  *
  *
  * If as RoutesGenerator is used: [[FileFromDirGenerator]] , then parameters argument must contains exactly one parameter.
  *
  * @author jubu
  */
class WebContentModule extends Module with Initializable with Runnable with Publisher {

  val CfgKey = "akkamo.webContent"

  private val Prefix = "web"

  private class RouteGeneratorDefinition(val `class`: String, val parameters: Option[List[String]])

  private class WebContentDefinition(
                                      val routeRegistryAlias: Option[String],
                                      val routeGenerators: Option[Map[String, RouteGeneratorDefinition]])

  override def initialize(ctx: Context) = Try {
    implicit val cfg: Config = ctx.get[Config]

    import eu.akkamo.m.config._ // need by parseConfig
    val parsed: List[Initializable.Parsed[WebContentDefinition]] =
      Initializable.parseConfig[WebContentDefinition](CfgKey).getOrElse {
        val prefix = Try {
          FileFromDirGenerator.toBaseSource(Prefix) // throws exception if not exists
          Prefix
        }.toOption
        Initializable.parseConfig[WebContentDefinition](CfgKey, ConfigFactory.parseString(default(prefix))).get
      }

    val registered = parsed.map { case (d, l, v) =>
      val generators = v.routeGenerators.getOrElse(Map.empty)
        .map { case (key, definition) =>
          val parameters = definition.parameters.getOrElse(List.empty).toArray
          val instance = newInstance(Class.forName(definition.`class`), parameters)
          (key, instance)
        }
      (d, l, new WebContentRegistry(v.routeRegistryAlias, generators))
    }
    ctx.register(Initializable.defaultReport(CfgKey, registered))
  }


  override def run(ctx: Context) = Try {
    val log = ctx.get[LoggingAdapterFactory].apply(this)
    ctx.registered[WebContentRegistry].foldLeft(ctx) {
      (ctx, r) =>
        val (wcr, _) = r
        // build routes
        val routes = wcr.mapping.map { case (prefix, rg) =>
          log.debug(s"generating route for prefix: ${prefix}")
          import akka.http.scaladsl.server.Directives._
          pathPrefix(prefix)(get(rg()))
        }
        // register routes
        routes.foldLeft(ctx) {
          (ctx, route) =>
            log.debug(s"register in route for alias: ${wcr.routeRegistryAlias}")
            ctx.registerIn[RouteRegistry, Route](route, wcr.routeRegistryAlias)
        }
    }
  }

  override def dependencies(ds: TypeInfoChain): TypeInfoChain = ds.&&[LoggingAdapterFactory].&&[Config].&&[RouteRegistry]

  override def publish(ds: TypeInfoChain): TypeInfoChain = ds.&&[WebContentRegistry]


  private def newInstance(clazz: Class[_], arguments: Array[String]): RouteGenerator =
    (if (arguments.length == 0) clazz.newInstance()
    else clazz.getConstructor(arguments.map(_.getClass): _*).newInstance(arguments: _*)).asInstanceOf[RouteGenerator]

  private def default(dir: Option[String]) = {
    dir.map(p =>
      s"""
         |akkamo.webContent = {
         |  default = {
         |    routeRegistryAlias = ${CfgKey.replace(".", "_")}
         |    routeGenerators = {
         |      ${p} = {
         |        class = "eu.akkamo.web.FileFromDirGenerator"
         |        parameters = ["${p}"]
         |      }
         |    }
         |  }
         |}
         """.stripMargin
    ).getOrElse(
      s"""
         |akkamo.webContent = {
         |  default = {
         |    routeRegistryAlias = ${CfgKey.replace(".", "_")}
         |  }
         |}
         """.stripMargin)
  }
}
