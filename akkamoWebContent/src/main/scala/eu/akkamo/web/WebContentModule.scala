package eu.akkamo.web

import akka.event.LoggingAdapter
import akka.http.scaladsl.server.Route
import com.typesafe.config.{Config, ConfigFactory}
import eu.akkamo.web.WebContentRegistry.{ContentMapping, RouteGenerator}
import eu.akkamo.{RouteRegistry, _}

import scala.util.Try


/**
  * Low level API is just map of prefix string to function: RequestContext=>Route
  *
  *
  */
trait WebContentRegistry extends Registry[ContentMapping] {
  def mapping: Map[String, RouteGenerator]

  def aliases: List[String]

  def routeRegistryAlias: Option[String]

  def default: Boolean
}

object WebContentRegistry {
  type RouteGenerator = () => Route
  type ContentMapping = (String, RouteGenerator)
}


/**
  * Provides serving of static data from resources, or selected directory.
  * Implementation of concrete handler cam be plugged in runtime, see: [[eu.akkamo.web.WebContentRegistry]]
  * or in declarative way, see `generators` section in example configuration, also [[eu.akkamo.web.FileFromDirGenerator]]
  * gives an example of route Generator
  *
  *
  * = Configuration example =
  *
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
  *       generators = [
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
  * if configuration doesn't exists equivalent of:
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
  * is created, if /web directory exsists in resources See: [[eu.akkamo.web.FileFromDirGenerator]] for more details
  *
  * @author JuBu
  *
  */
class WebContentModule extends Module with Initializable with Runnable {

  import config._

  val WebContentModuleKey = "akkamo.webContent"

  val RouteRegistryAlias = "routeRegistryAlias"

  private val Aliases = "aliases"

  private val Default = "default"

  private val RouteGenerators = "routeGenerators"


  case class
  DefaultWebContentRegistry(aliases: List[String],
                            routeRegistryAlias: Option[String],
                            default: Boolean,
                            override val mapping: Map[String, RouteGenerator] = Map.empty) extends WebContentRegistry {

    /**
      * Creates new instance of particular [[Registry]] implementation, with the new given value.
      *
      * @param p new value, map prefix -> RouteGenerator
      * @return new instance of particular [[Registry]] implementation
      */
    override def copyWith(p: (String, RouteGenerator)): DefaultWebContentRegistry.this.type = {
      if (mapping.contains(p._1)) throw ContextError(s"A RouteGenerator under kee:${p._1} already registered ")
      else this.copy(mapping = mapping + p).asInstanceOf[this.type]
    }


  }

  override def initialize(ctx: Context) = Try {
    val cfg: Config = ctx.inject[Config].get
    val log: LoggingAdapter = ctx.inject[LoggingAdapterFactory].map(_ (this)).get

    log.info("Initializing 'WebContent' module...")

    val mpOption = get[Map[String, Config]](WebContentModuleKey, cfg)

    // check  exsistence of default
    if (mpOption.isEmpty && !Try(FileFromDirGenerator.defaultUri).isSuccess) {
      log.warning("Can't find default 'WebContent' resource, or directory.")
      ctx
    } else {
      initialize(ctx, mpOption.getOrElse(makeDefault))
    }
  }


  override def run(ctx: Context) = Try {
    import akka.http.scaladsl.server.Directives._
    ctx.registered[WebContentRegistry].foldLeft(ctx) { (ctx, r) =>
      val (wcr, _) = r
      // build routes
      val routes = wcr.mapping.map { case (prefix, rg) => pathPrefix(prefix)(get(rg())) }
      // register routes
      routes.foldLeft(ctx) { (ctx, route) =>
        ctx.registerIn[RouteRegistry, Route](route, wcr.routeRegistryAlias)
      }
    }
  }

  override def dependencies(dependencies: Dependency): Dependency =
    dependencies.&&[LogModule].&&[ConfigModule].&&[AkkaHttpModule]

  private def initialize(ctx: Context, mp: Map[String, Config]) = {
    val autoDefault = mp.size == 1
    val rrs = mp.map { case (key, cfg) =>
      val routeRegistryAlias = get[String](RouteRegistryAlias, cfg)
      val aliases = key :: get[List[String]](Aliases, cfg).getOrElse(List.empty[String])
      val default = get[Boolean](Default, cfg).getOrElse(autoDefault)
      val generators = getRouteGenerators(get[List[Config]](RouteGenerators, cfg).getOrElse(List.empty)).toMap
      DefaultWebContentRegistry(aliases, routeRegistryAlias, default, generators)
    }
    // more checks - like one default, distinguish mappings in generators ...
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

  private def getRouteGenerators(p: List[Config]) = p.map { cfg =>
    val className = get[String]("class", cfg).getOrElse(classOf[FileFromDirGenerator].getName)
    val parameters = get[List[String]]("parameters", cfg).getOrElse(List.empty).toArray
    val prefix = get[String]("prefix", cfg).getOrElse(FileFromDirGenerator.Prefix)
    (prefix, newInstance(Class.forName(className), parameters))
  }

  private def newInstance(clazz: Class[_], arguments: Array[String]): RouteGenerator = (if (arguments.length == 0) {
    clazz.newInstance()
  } else {
    clazz.getConstructor(arguments.map(_.getClass): _*).newInstance(arguments: _*)
  }).asInstanceOf[RouteGenerator]

  private def makeDefault = Map(
    "default" -> ConfigFactory.parseString(
      s"""
         |default = {
         |  akkaHttpAlias="akkamo.webContent"
         |  default = true
         |  routeGenerators = [{
         |   prefix=${FileFromDirGenerator.Prefix}
         |   class = eu.akkamo.web.FileFromDirGenerator
         |   }]
         |}
         |uri = "mongodb://localhost/default"
         | """.stripMargin))


}