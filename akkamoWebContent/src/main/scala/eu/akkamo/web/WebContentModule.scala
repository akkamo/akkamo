package eu.akkamo.web

import akka.event.LoggingAdapter
import akka.http.scaladsl.server.{RequestContext, Route}
import com.typesafe.config.{Config, ConfigFactory}
import eu.akkamo._
import eu.akkamo.web.WebContentRegistry.{ContentMapping, RouteGenerator}

import scala.collection.Map
import scala.util.Try


/**
  * Low level API is just map of prefix string to function: RequestContext=>Route
  *
  *
  */
trait WebContentRegistry extends Registry[ContentMapping] {
  def mapping: Map[String, RouteGenerator]
}

object WebContentRegistry {
  type RouteGenerator = RequestContext => Route
  type ContentMapping = (Option[String], RouteGenerator)
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
  *     // complete configuration with several name aliases
  *     name1 = {
  *       akkaHttpAlias = "akkaHttpAlias"
  *       aliases = ["alias1", "alias2"]
  *       prefix = "prefix1" // registered url prefix
  *     },
  *     // configuration registered as default (only one instance is allowed)
  *     name2 = {
  *       default = true // at least in one configuration is value mandatory if the number of instances is > 1
  *       prefix = "prefix2"
  *       generators = [
  *         {
  *          prefix = "www" // mandatory regular expression if more generators is used
  *          class = eu.akkamo.web.FileFromDirGenerator
  *          parameters = ["/www", "true"]
  *         }
  *       ]
  *     }
  *   }
  * }}}
  *
  * if configuration doesn't exists equivalent of:
  * {{{
  *   default = {
  *     akkaHttpAlias="akkamo.webContent"
  *     prefix = "/web"
  *     default = true
  *     routeGenerators = [
  *         { class = eu.akkamo.web.FileFromDirGenerator }
  *      ]
  *   }
  * }}}
  * is created, if /web directory exsists in resources See: [[eu.akkamo.web.FileFromDirGenerator]] for more details
  *
  * @author JuBu
  *
  */
class WebContentModule extends Module with Initializable with Runnable  {

  val WebContentModuleKey = "akkamo.webContent"

  val AkkaHttpAlias = "akkaHttpAlias"

  private val Aliases = "aliases"

  private val Prefix = "prefix"

  private val Default = "default"


  case class
  DefaultWebContentRegistry(aliases: List[String],
                            akkaHttpAlias: Option[String],
                            prefix:String,
                            default: Boolean,
                            override val mapping: Map[Option[String], RouteGenerator] = Map.empty) extends WebContentRegistry {

    /**
      * Creates new instance of particular [[Registry]] implementation, with the new given value.
      *
      * @param p new value
      * @return new instance of particular [[Registry]] implementation
      */
    override def copyWith(p: (Option[String], RouteGenerator)): DefaultWebContentRegistry.this.type = {
      if (mapping.contains(p._1)) throw ContextError(s"A RouteGenerator under kee:${p._1} already registered ")
      else this.copy(mapping = mapping + p).asInstanceOf[this.type]
    }
  }

  override def initialize(ctx: Context) = Try {
    val cfg: Config = ctx.inject[Config].get
    val log: LoggingAdapter = ctx.inject[LoggingAdapterFactory].map(_ (this)).get

    log.info("Initializing 'WebContent' module...")

    val mp = config.get[Map[String, Config]](WebContentModuleKey, cfg).getOrElse(makeDefault)
    val autoDefault = mp.size == 1
    mp.map { case (key, cfg) =>
      val akkaHttpAlias = config.get[String](AkkaHttpAlias, cfg)
      val aliases = key :: config.get[List[String]](Aliases, cfg).getOrElse(List.empty[String])
      val prefix = config.get[String](Prefix, cfg).getOrElse(
        throw InitializableError(s"parameter prefix is mandatory for web content configuration key:$key"))
      val default = config.get[Boolean](Default, cfg).getOrElse(autoDefault)
    }

    ctx

  }


  override def run(ctx: Context) = {ctx}

  override def dependencies(dependencies: Dependency): Dependency =
    dependencies.&&[LogModule].&&[ConfigModule].&&[AkkaHttpModule]

  private def existDefaultSources =

  private def makeDefault = Map(
    "default" -> ConfigFactory.parseString(
      """
        |default = {
        |  akkaHttpAlias="akkamo.webContent"
        |  prefix = "/web"
        |  default = true
        |  routeGenerators = [{ class = eu.akkamo.web.FileFromDirGenerator }]
        |}
        |uri = "mongodb://localhost/default"
        | """.stripMargin))



}