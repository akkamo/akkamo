package eu.akkamo

import java.security.{KeyStore, SecureRandom}
import javax.net.ssl.{KeyManagerFactory, SSLContext, TrustManagerFactory}

import akka.actor.ActorSystem
import akka.event.{Logging, LoggingAdapter}
import akka.http.scaladsl.Http.ServerBinding
import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.RouteResult.{Complete, Rejected}
import akka.http.scaladsl.server.directives.{DebuggingDirectives, LogEntry, LoggingMagnet}
import akka.http.scaladsl.{ConnectionContext, Http, HttpsConnectionContext}
import akka.stream.ActorMaterializer
import com.typesafe.config.Config
import eu.akkamo.RouteRegistry.{HTTP, HTTPS, Protocol}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

/**
  * For each configuration block, an instance of [[RouteRegistry]] is registered to the
  * ''Akkamo context'' using its name, aliases (if available) or as default (is specified so).
  * [[RouteRegistry]] serves as main interface for configured ''Akka HTTP'' server, allowing to
  * register own ''Akka HTTP'' routes.
  *
  * @author jubu
  */
trait RouteRegistry extends Registry[Route] {

  /**
    * Returns port number, on which the ''Akka HTTP'' server is running.
    *
    * @return port number
    */
  def port: Int

  /**
    * Returns the interface (i.e. host name), on which the ''Akka HTTP'' server is running.
    *
    * @return interface (i.e. host name)
    */
  def interface: String

  /**
    * Returns the protocol (e.g. HTTP, HTTPS).
    *
    * @return the protocol
    */
  def protocol: Protocol

  /**
    * Returns whether the current ''Akka HTTP'' server configuration is the default one.
    *
    * @return `true` if the current ''Akka HTTP'' server configuratino is the default one
    */
  def default: Boolean
}

/**
  * Companion object for the [[RouteRegistry]] trait.
  */
object RouteRegistry {

  /**
    * Represents the protocol (e.g. HTTP, HTTPS).
    */
  sealed trait Protocol {

    /**
      * Returns the protocol name.
      *
      * @return protocol name
      */
    def name = toString
  }

  /**
    * Implementation of [[Protocol]], representing the HTTP protocol.
    */
  case object HTTP extends Protocol {
    override def toString: String = "http"
  }

  /**
    * Implementation of [[Protocol]], representing the HTTPS protocol.
    */
  case object HTTPS extends Protocol {
    override def toString: String = "https"
  }

}

// TODO - documentation details about ssl config + factory: HttpsConnectionContextFactory description
/**
  * Module providing HTTP(S) server functionality, based on the ''Akka HTTP'' library.
  *
  * = Configuration example =
  * {{{
  *   akkamo.akkaHttp = {
  *     // complete configuration with several name aliases
  *     name1 = {
  *       aliases = ["alias1", "alias2"]
  *       port = 9000 // port, not mandatory
  *       protocol = "http" // http, https, ...
  *       host = "localhost" // host, default localhost
  *       akkaAlias = "alias" // not required, default is used if exists
  *       requestLogLevel = "info"  // defines level for request level logging. Default "off" means no logging
  *       requestLogFormat = "%{HTTP_METHOD} %{REQ_URI}: HTTP/%{RESP_STATUS}" // defines log format, defaults to this if not specified
  *     },
  *     // configuration registered as default (only one instance is allowed)
  *     name2 = {
  *       default = true
  *       protocol = "http" // http, https, ...
  *     }
  *   }
  * }}}
  *
  * In example code above, working example of module configuration is shown. For each block inside
  * the `akkamo.akkaHttp`, instance of [[RouteRegistry]] is registered to the ''Akkamo'' context
  * both using its name (e.g. ''name1'') and aliases (e.g. ''alias1'') if provided. Injected
  * instance of [[RouteRegistry]] can be then used to register own Akka HTTP routes.
  *
  * @author jubu
  * @see RouteRegistry
  * @see http://doc.akka.io/docs/akka/current/scala/http/
  */
class AkkaHttpModule extends Module with Initializable with Runnable with Disposable {

  import config._

  private val AkkaHttpKey = "akkamo.akkaHttp"

  private val Protocol = "protocol"

  private val Port = "port"

  private val Interface = "interface"

  private val RequestLogLevel = "requestLogLevel"

  private val RequestLogFormat = "requestLogFormat"

  private val AkkaAlias = "akkaAlias"

  private val Aliases = "aliases"

  private val Default = "default"

  private val KeyStorePassword = "keyStorePassword"

  private val KeyStoreName = "keyStorePassword"

  private val KeyStoreLocation = "keyStorePassword"

  private val KeyManagerAlgorithm = "keyManagerAlgorithm"

  private val SSLContextAlgorithm = "sSLContext Algorithms"

  private val HttpsConnectionContextFactoryClassName = "httpsConnectionContextFactoryClassName"

  type HttpsConnectionContextFactory = (Config) => HttpsConnectionContext

  private type ServerBindingGetter = () => Future[ServerBinding]

  private var bindings = List.empty[ServerBinding]


  private[AkkaHttpModule] trait BaseRouteRegistry extends ServerBindingGetter with RouteRegistry {

    val routes: Set[Route]

    def apply(): Future[ServerBinding] = {
      import akka.http.scaladsl.server.RouteConcatenation
      val rts: Route = RouteConcatenation.concat(routes.toList: _*)
      bind(rts)
    }

    def bind(route: Route): Future[ServerBinding]

    def aliases: List[String]

    override def toString() = {
      s"${this.getClass.getSimpleName}(aliases=$aliases, uri=$interface:$port, isDefault=$default)"
    }
  }

  private[AkkaHttpModule] case class
  HttpRouteRegistry(aliases: List[String], port: Int, interface: String, default: Boolean,
                    requestLogLevel: String, requestLogFormat: String, routes: Set[Route] = Set.empty)
                   (implicit as: ActorSystem) extends BaseRouteRegistry {

    def bind(route: Route): Future[ServerBinding] = {
      implicit val am = ActorMaterializer()
      import scala.concurrent.ExecutionContext.Implicits.global
      if (requestLogLevel == "off") {
        Http().bindAndHandle(route, interface, port)
      } else {
        val myLoggedRoute = logRequestResult(requestLogLevel, requestLogFormat, route)
        Http().bindAndHandle(myLoggedRoute, interface, port)
      }
    }

    def copyWith(p: Route) = {
      this.copy(routes = routes + p).asInstanceOf[this.type]
    }

    val protocol = HTTP
  }

  private[AkkaHttpModule] case class
  HttpsRouteRegistry(aliases: List[String], port: Int, interface: String, default: Boolean,
                     ctx: HttpsConnectionContext, requestLogLevel: String, requestLogFormat: String,
                     routes: Set[Route] = Set.empty)
                    (implicit as: ActorSystem) extends BaseRouteRegistry {

    def bind(route: Route): Future[ServerBinding] = {
      implicit val am = ActorMaterializer()
      import scala.concurrent.ExecutionContext.Implicits.global
      if (requestLogLevel == "off") {
        Http().bindAndHandle(route, interface, port, ctx)
      } else {
        val myLoggedRoute = logRequestResult(requestLogLevel, requestLogFormat, route)
        Http().bindAndHandle(myLoggedRoute, interface, port, ctx)
      }
    }

    def copyWith(p: Route) = {
      this.copy(routes = routes + p).asInstanceOf[this.type]
    }

    val protocol = HTTPS
  }

  /**
    * register module mappings
    *
    * @param ctx Akkamo context
    * @return true if initialization is complete.
    *         In case of incomplete initialization system will call this method again.
    *         Incomplete initialization mean That component is not able to find all dependencies.
    */
  override def initialize(ctx: Context) = {
    val cfg = ctx.inject[Config]
    val log = ctx.inject[LoggingAdapterFactory].map(_ (this))
    initialize(ctx, cfg.get, log.get)
  }

  def initialize(ctx: Context, cfg: Config, log: LoggingAdapter) = Try {
    import config._

    // create list of configuration tuples
    val mp = get[Map[String, Config]](AkkaHttpKey, cfg)

    val httpConfigs = if (mp.isEmpty) {
      val r = HttpRouteRegistry(Nil, 9000, "localhost", default = true, "off", defaultLogFormat)(
        ctx.inject[ActorSystem].getOrElse(throw InitializableError("Can't find default akka system")))
      List(r)
    } else {
      val autoDefault = mp.get.size == 1
      mp.get.toList.filter(_._1 != RequestLogLevel).map { case (key, conf) =>
        val system = config.get[String](AkkaAlias, conf).flatMap(ctx.inject[ActorSystem](_)).orElse(ctx.inject[ActorSystem])
        if (system.isEmpty) {
          throw InitializableError(s"Can't find akka system for http configuration key: $key")
        }
        val protocol = config.get[String](Protocol, conf).getOrElse("http")
        val port = config.get[Int](Port, conf).getOrElse(-1)
        val interface = config.get[String](Interface, conf).getOrElse("localhost")
        val aliases = key :: config.get[List[String]](Aliases, conf).getOrElse(List.empty[String])
        val default = config.get[Boolean](Default, conf).getOrElse(autoDefault)
        val requestLogLevel: String = config.get[String](RequestLogLevel, conf).getOrElse("off")
        val requestLogFormat: String = config.get[String](RequestLogFormat, conf).getOrElse(defaultLogFormat)
        protocol.toLowerCase match {
          case "http" =>
            val r = HttpRouteRegistry(aliases, port, interface, default,
              requestLogLevel, requestLogFormat)(system.get)
            log.info(s"created: $r ")
            r
          case "https" =>
            val r = HttpsRouteRegistry(aliases, port, interface, default, getHttpsConnectionContext(conf),
              requestLogLevel, requestLogFormat)(system.get)
            log.info(s"created: $r ")
            r
          case p => throw InitializableError(s"unknown protocol:$p in route registry, see: $config")
        }
      }
    }
    val combinations = httpConfigs.groupBy(_.interface).map(_._2.groupBy(_.port).size).sum
    if (combinations != httpConfigs.size) {
      throw InitializableError(s"Akka http configuration contains ambiguous combination of port and protocol.")
    }
    httpConfigs.foldLeft(ctx) { (ctx, cfg) =>
      val ctx1 = if (cfg.default) {
        ctx.register[RouteRegistry](cfg)
      } else {
        ctx
      }
      cfg.aliases.foldLeft(ctx1) { (ctx, alias) =>
        ctx.register[RouteRegistry](cfg, Some(alias))
      }
    }
  }

  def logRequestResult(logLevel: String, logFormat: String, route: Route)
                      (implicit ec: ExecutionContext): Route = {

    def requestLog(request: HttpRequest, status: String, log: LoggingAdapter): String = {
      val replacements: Map[String, String] = Map(
        "HTTP_METHOD" -> request.method.value,
        "REQ_URI" -> request.uri.toString(),
        "RESP_STATUS" -> status
      )

      ParamsReplacer.replaceParams(logFormat, replacements, Some(log))
    }

    def myLoggingFunction(logger: LoggingAdapter)(req: HttpRequest)(res: Any): Unit = {

      val level = Logging.levelFor(logLevel).getOrElse(Logging.DebugLevel)
      val entry = res match {
        case Complete(resp) =>
          Future.successful(LogEntry(requestLog(req, resp.status.toString(), logger), level))
        case Rejected(rejections) if rejections.isEmpty =>
          Future.successful(LogEntry(requestLog(req, "404 Not Found", logger), level))
        case Rejected(rejections) =>
          Future.successful(LogEntry(requestLog(req, "400 Bad Request", logger), level))
        case other => Future.successful(LogEntry(s"Other: $other", level))
      }
      entry.foreach(_.logTo(logger))
    }
    DebuggingDirectives.logRequestResult(LoggingMagnet(log => myLoggingFunction(log)))(route)
  }

  private def defaultLogFormat: String = "%{HTTP_METHOD} %{REQ_URI}: HTTP/%{RESP_STATUS}"

  override def dependencies(dependencies: Dependency): Dependency = dependencies.&&[ConfigModule].&&[LogModule].&&[AkkaModule]

  override def run(ctx: Context) = {
    import scala.concurrent.ExecutionContext.Implicits.global
    val log = ctx.inject[LoggingAdapterFactory].map(_ (this)).get
    val httpConfigs = ctx.registered[RouteRegistry].keySet.map(_.asInstanceOf[BaseRouteRegistry])
    val futures = httpConfigs.map(p => p().transform(p => p, th => RunnableError(s"Can`t initialize route $p", th)))
    Future.sequence(futures).map { p =>
      log.info(s"run: $httpConfigs")
      ctx
    }
  }

  override def dispose(ctx: Context) = {
    import scala.concurrent.ExecutionContext.Implicits.global
    val futures = bindings.map(p => p.unbind().transform(p => p, th => DisposableError(s"Can`t initialize route $p", th)))
    Future.sequence(futures).map { p => () }
  }

  private def getHttpsConnectionContext(cfg: Config): HttpsConnectionContext =
    get[String](HttpsConnectionContextFactoryClassName, cfg)
      .map(getHttpsConnectionContextFormFactory(cfg)).getOrElse(getHttpsConnectionContextFormConfig(cfg))


  private def getHttpsConnectionContextFormFactory(cfg: Config) = (clazzName: String) => {
    try {
      import scala.reflect.runtime.universe
      val runtimeMirror = universe.runtimeMirror(getClass.getClassLoader)
      val module = runtimeMirror.staticModule(clazzName)
      val companionObj = runtimeMirror.reflectModule(module).instance.asInstanceOf[HttpsConnectionContextFactory]
      companionObj(cfg)
    } catch {
      case th: Throwable => throw InitializableError(s"Can't construct HttpsConnectionContext from the factory: $clazzName", th)
    }
  }

  private def getHttpsConnectionContextFormConfig(implicit cfg: Config) = {
    val keyStoreName = config.get[String](KeyStoreName).getOrElse(
      throw InitializableError("Can't find keyStoreName value"))
    val keyStorePassword = config.get[String](KeyStorePassword).getOrElse(
      throw InitializableError("Can't find keyStorePassword value")).toCharArray
    val sslCtx = SSLContext.getInstance("TLS")
    val keyStore = Option(KeyStore.getInstance(keyStoreName)).getOrElse(
      throw InitializableError(s"Can't initialize key store for keyStoreName: $keyStoreName"))
    val keyStoreStream = getClass.getClassLoader.getResourceAsStream(config.get[String](KeyStoreLocation).getOrElse("server.p12"))
    keyStore.load(keyStoreStream, keyStorePassword)
    val keyManagerFactory = KeyManagerFactory.getInstance(get[String](KeyManagerAlgorithm).getOrElse(KeyManagerFactory.getDefaultAlgorithm))
    keyManagerFactory.init(keyStore, keyStorePassword)
    val trustManagerFactory: TrustManagerFactory = TrustManagerFactory.getInstance(keyManagerFactory.getAlgorithm)
    trustManagerFactory.init(keyStore)
    val sslContext: SSLContext = get[String](SSLContextAlgorithm).map(SSLContext.getInstance).getOrElse(SSLContext.getDefault)
    sslContext.init(keyManagerFactory.getKeyManagers, trustManagerFactory.getTrustManagers, SecureRandom.getInstanceStrong)
    ConnectionContext.https(sslContext)
  }
}
