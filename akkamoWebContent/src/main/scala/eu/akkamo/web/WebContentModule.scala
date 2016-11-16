package eu.akkamo.web

import akka.http.scaladsl.server.Route
import com.typesafe.config.{Config, ConfigFactory}
import eu.akkamo.web.WebContentRegistry.{ContentMapping, RouteGenerator}
import eu.akkamo.{RouteRegistry, _}

import scala.util.Try

/**
  * Represents registry for single configured static content mapping.
  */
trait WebContentRegistry extends Registry[ContentMapping] {

  /**
    * Mapping of route generators.
    *
    * @return route generators mapping
    */
  def mapping: Map[String, RouteGenerator]

  /**
    * List of aliases, under which this content mapping registry is registered into the
    * ''Akkamo context''.
    *
    * @return content mapping registry aliases
    */
  def aliases: List[String]

  /**
    * Optional value of the Akka HTTP module's registered `RouteRegistry`
    *
    * @return Akka HTTP module's registered `RouteRegistry` alias
    */
  def routeRegistryAlias: Option[String]

  /**
    * Whether this content mapping registry is registered as the `default` service instance.
    *
    * @return `true` if this content mapping registry is registered as the `default` service
    *         instance
    */
  def default: Boolean
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
  *       routeGenerators = [
  *         {
  *          prefix="web"
  *          class = eu.akkamo.web.FileFromDirGenerator
  *          parameters = ["/web"]
  *         }
  *       ]
  *     }
  *   }
  * }}}
  *
  * If no configuration is provided, default configuration equivalent to configuration snipped
  * below is used, trying to serve the static content from `/web` directory, either found at
  * classpath or file system (see [[FileFromDirGenerator]] for more details):
  *
  * {{{
  *   default = {
  *     routeRegistryAlias="akkamo.webContent"
  *     default = true
  *     routeGenerators = [
  *         {
  *           class = eu.akkamo.web.FileFromDirGenerator
  *         }
  *      ]
  *   }
  * }}}
  *
  * @author jubu
  */
class WebContentModule extends Module with Initializable with Runnable {

  import config._

  val WebContentModuleKey = "akkamo.webContent"

  val RouteRegistryAlias = "routeRegistryAlias"

  private val Aliases = "aliases"

  private val Default = "default"

  private val RouteGenerators = "routeGenerators"

  private val Clazz = "class"

  private val Prefix = "prefix"


  case class
  DefaultWebContentRegistry(aliases: List[String],
                            routeRegistryAlias: Option[String],
                            default: Boolean,
                            override val mapping: Map[String, RouteGenerator] = Map.empty) extends WebContentRegistry {


    override def copyWith(p: (String, RouteGenerator)): DefaultWebContentRegistry.this.type = {
      if (mapping.contains(p._1)) throw ContextError(s"A RouteGenerator under key: ${p._1} is already registered")
      else this.copy(mapping = mapping + p).asInstanceOf[this.type]
    }


  }

  override def initialize(ctx: Context) = Try {
    val cfg: Config = ctx.inject[Config].get

    val mpOption = get[Map[String, Config]](WebContentModuleKey, cfg)

    val useGenerators = mpOption.isEmpty && Try(FileFromDirGenerator.defaultBaseSource).isSuccess
    // check  existence of default
    val mp = mpOption.getOrElse(defaultFile(useGenerators))
    val autoDefault = mp.size == 1
    val rrs = mp.map { case (key, conf) =>
      val routeRegistryAlias = get[String](RouteRegistryAlias, conf)
      val aliases = key :: get[List[String]](Aliases, conf).getOrElse(List.empty[String])
      val default = get[Boolean](Default, conf).getOrElse(autoDefault)
      val generators = get[List[Config]](RouteGenerators, conf).map(getRouteGenerators).getOrElse(List.empty).toMap
      DefaultWebContentRegistry(aliases, routeRegistryAlias, default, generators)
    }

    // check if only one default content registry is specified
    val defaultsNo: Int = rrs.groupBy(_.default).get(true).map(_.size).getOrElse(0)
    if (defaultsNo > 1) {
      throw InitializableError("Only one content registry can be marked as `default`")
    }

    rrs.foldLeft(ctx) { (ctx, registry) =>
      val ctx1 = if (registry.default) {
        ctx.register[WebContentRegistry](registry)
      } else {
        ctx
      }
      registry.aliases.foldLeft(ctx1) { (ctx, alias) =>
        ctx.register[WebContentRegistry](registry, Some(alias))
      }
    }
  }


  override def run(ctx: Context) = Try {
    import akka.http.scaladsl.server.Directives._
    val log = ctx.inject[LoggingAdapterFactory].map(_ (this)).get

    ctx.registered[WebContentRegistry].foldLeft(ctx) { (ctx, r) =>
      val (wcr, _) = r
      // build routes
      val routes = wcr.mapping.map { case (prefix, rg) =>
        log.debug(s"generating route for ${Prefix}: ${prefix}")
        pathPrefix(prefix)(get(rg()))
      }
      // register routes
      routes.foldLeft(ctx) { (ctx, route) =>
        log.debug(s"register in route for key: ${wcr.routeRegistryAlias}")
        ctx.registerIn[RouteRegistry, Route](route, wcr.routeRegistryAlias)
      }
    }
  }

  override def dependencies(dependencies: Dependency): Dependency =
    dependencies.&&[LogModule].&&[ConfigModule].&&[AkkaHttpModule]

  private def getRouteGenerators(p: List[Config]) = p.map { cfg =>
    val className = get[String](Clazz, cfg).getOrElse(classOf[FileFromDirGenerator].getName)
    val parameters = get[List[String]]("parameters", cfg).getOrElse(List.empty).toArray
    val prefix = get[String](Prefix, cfg).getOrElse(FileFromDirGenerator.Prefix)
    (prefix, newInstance(Class.forName(className), parameters))
  }

  private def newInstance(clazz: Class[_], arguments: Array[String]): RouteGenerator = (if (arguments.length == 0) {
    clazz.newInstance()
  } else {
    clazz.getConstructor(arguments.map(_.getClass): _*).newInstance(arguments: _*)
  }).asInstanceOf[RouteGenerator]

  private def defaultFile(useGenerators: Boolean) = {
    val generators =
      if (useGenerators)
        s"""
           |$RouteGenerators = [{
           |  $Prefix=${FileFromDirGenerator.Prefix}
           |  $Clazz = eu.akkamo.web.FileFromDirGenerator
           |}]
         """.stripMargin
      else ""
    Map(
      WebContentModuleKey -> ConfigFactory.parseString(
        s"""
           |{
           |  $RouteRegistryAlias=$WebContentModuleKey
           |  $Default = true
           |$generators
           |}""".stripMargin))
  }
}
