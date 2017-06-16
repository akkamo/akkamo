package eu.akkamo

import java.io.{File, FileInputStream, InputStream}
import java.security.{KeyStore, SecureRandom}
import javax.net.ssl.{KeyManagerFactory, SSLContext, TrustManagerFactory}

import akka.actor.ActorSystem
import akka.event.Logging.{LogLevel, MDC}
import akka.event.{DefaultLoggingFilter, DiagnosticMarkerBusLoggingAdapter, LogSource, Logging, LoggingBus, LoggingFilter}
import akka.http.scaladsl.Http.ServerBinding
import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.server.RouteResult.{Complete, Rejected}
import akka.http.scaladsl.server.directives.BasicDirectives.mapInnerRoute
import akka.http.scaladsl.server.directives.{DebuggingDirectives, LoggingMagnet}
import akka.http.scaladsl.server.{Directive, Route, RouteResult}
import akka.http.scaladsl.{ConnectionContext, Http, HttpsConnectionContext}
import akka.stream.ActorMaterializer
import com.typesafe.config.{Config, ConfigFactory, ConfigObject, ConfigValue, ConfigValueType}

import scala.concurrent.Future

/**
  * For each configuration block, an instance of [[RouteRegistry]] is registered to the
  * ''Akkamo context'' using its name, aliases (if available) or as default (is specified so).
  * [[RouteRegistry]] serves as main interface for configured ''Akka HTTP'' server, allowing to
  * register own ''Akka HTTP'' routes.
  *
  * @author jubu
  */
trait RouteRegistry extends Registry[Route]

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
  *       interface = "localhost" // host, default localhost
  *       akkaAlias = "alias" // not required, default is used if exists
  *       requestLogLevel = "info"  // defines level for request level logging. Default "off" means no logging
  *       useMDC = false // defines usage of logger than support custom MDC, in this case headers mapped  in to MDC
  *       requestLogFormat = "%1s %2s: HTTP/%3s headers:%4s" // defines log format, defaults to this if not specified
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
  * <br/>
  * log format:
  * <ol>
  * <li>method</li>
  * <li>relative uri</li>
  * <li>status</li>
  * <li>headers (only if use MDC is false)</li>
  * </ol>
  *
  * @author jubu
  * @see RouteRegistry
  * @see http://doc.akka.io/docs/akka/current/scala/http/
  */
class AkkaHttpModule extends Module with Initializable with Runnable with Disposable with Publisher {

  import eu.akkamo.Initializable.Interceptor
  import eu.akkamo.m.config._ // need by parseConfig

  val CfgKey = "akkamo.akkaHttp"


  val default =
    s"""
       |$CfgKey = {
       | default = { }
       |}
    """.stripMargin

  private val Protocol = "protocol"

  private val AkkaAlias = "akkaAlias"

  private val SSLCfg = "sslCfg"

  type HttpsConnectionContextFactory = (Config) => HttpsConnectionContext

  private val bindings = List.empty[ServerBinding]

  private case class
  RouteRegistryData(port: Int = -1,
                    interface: String = "localhost",
                    protocol: String = "http"
                   )
                   (
                     val requestLogLevel: String = "info",
                     val requestLogFormat: String = "%1s %2s: HTTP/%3s headers:%4s",
                     val useMDC: Boolean = false
                   )


  private case class
  RouteRegistryImpl(data: RouteRegistryData, routes: Set[Route] = Set.empty)
                   (implicit as: ActorSystem, cc: ConnectionContext) extends RouteRegistry {

    type SelfType = RouteRegistryImpl

    def logDirective(level: LogLevel, formater: (String, HttpRequest) => String) = {

      def loggingFunction(logger: akka.event.LoggingAdapter)(req: HttpRequest)(res: RouteResult): Unit = {
        val status = res match {
          case c: Complete => c.response.status.toString
          case r: Rejected => if (r.rejections.isEmpty) "404 Not Found" else "400 Bad Request"
        }
        logger.log(level, formater(status, req))
      }

      DebuggingDirectives.logRequestResult(LoggingMagnet(log => loggingFunction(log)))
    }

    def mdcLogger(filter: LoggingFilter, logSource: String, clazz: Class[_], loggingBus: LoggingBus): Directive[Unit] = {
      mapInnerRoute { route =>
        ctx => {
          val mdc: MDC = ctx.request.headers.map(h => (h.name(), h.value.asInstanceOf[Any])).toMap
          val dlog = new DiagnosticMarkerBusLoggingAdapter(loggingBus, logSource, clazz, filter)
          dlog.mdc(mdc)
          route(ctx.reconfigure(log = dlog))
        }
      }
    }

    def apply(): Future[ServerBinding] = {
      import akka.http.scaladsl.server.RouteConcatenation
      val rts: Route = RouteConcatenation.concat(routes.toList: _*)
      bind(rts)
    }

    def bind(route: Route): Future[ServerBinding] = {
      implicit val am = ActorMaterializer()
      import scala.concurrent.ExecutionContext.Implicits.global
      if (data.requestLogLevel == "off") {
        Http().bindAndHandle(route, data.interface, data.port, cc)
      } else {
        // optimized formatter for each variant
        // TODO rethink this kind of decision is
        val formatter = if (data.requestLogFormat.contains("%4s")) {
          (status: String, req: HttpRequest) => {
            val method = req.method.value
            val uri = req.uri.toRelative.toString
            val headers = req.headers.mkString(",")
            data.requestLogFormat.format(method, uri, status, headers)
          }
        } else {
          (status: String, req: HttpRequest) => {
            val method = req.method.value
            val uri = req.uri.toRelative.toString
            data.requestLogFormat.format(method, uri, status)
          }
        }
        val ld = logDirective(Logging.levelFor(data.requestLogLevel).getOrElse(Logging.InfoLevel), formatter)
        val finalRoute = if (data.useMDC) {
          val (logSource, clazz) = LogSource.fromAnyRef(classOf[RouteRegistry], as)
          val filter = new DefaultLoggingFilter(as.settings, as.eventStream)
          mdcLogger(filter, logSource, clazz, as.eventStream)(ld(route))
        } else {
          ld(route)
        }
        Http().bindAndHandle(finalRoute, data.interface, data.port, cc)
      }
    }

    override def copyWith(p: Route): SelfType = {
      copy(routes = this.routes + p)(as, cc)
    }
  }


  private case class
  SSLConfig(
             keyStoreName: String,
             keyStorePassword: String,
             keyStoreLocation: String = "server.p12",
             keyManagerAlgorithm: String = KeyManagerFactory.getDefaultAlgorithm,
             sSLContextAlgorithm: Option[String] = None
           ) {


    val sSLContext: SSLContext = {

      val keyStore: KeyStore = {
        val keyStoreStream = getResource(keyStoreLocation)
        val keyStore = KeyStore.getInstance(keyStoreName)
        keyStore.load(keyStoreStream, keyStorePassword.toCharArray)
        keyStore
      }

      val keyManagerFactory: KeyManagerFactory = {
        val instance = KeyManagerFactory.getInstance(keyManagerAlgorithm)
        instance.init(keyStore, keyStorePassword.toCharArray)
        instance
      }

      val trustManagerFactory: TrustManagerFactory = TrustManagerFactory.getInstance(keyManagerFactory.getAlgorithm)
      val sSLContext = sSLContextAlgorithm.map(SSLContext.getInstance).getOrElse(SSLContext.getDefault)
      sSLContext.init(keyManagerFactory.getKeyManagers, trustManagerFactory.getTrustManagers, SecureRandom.getInstanceStrong)
      sSLContext
    }
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

    val cfg = ctx.get[Config]

    val ir = interceptor(ctx)

    val registered: List[Initializable.Parsed[RouteRegistry]] =
      Initializable.parseConfig(CfgKey, cfg, ir).getOrElse {
        Initializable.parseConfig(CfgKey, ConfigFactory.parseString(default), ir).get
      }
    ctx.register(Initializable.defaultReport(CfgKey, registered))

  }

  private def interceptor(ctx: Context): Interceptor[RouteRegistryData, RouteRegistryImpl] =
    (t: Transformer[RouteRegistryData], v: ConfigValue) => {
      if (v.valueType() != ConfigValueType.OBJECT) {
        throw new IllegalArgumentException(s"The value: $v is not `OBJECT`. Can't be parsed to type: ${classOf[RouteRegistry].getName}")
      }
      implicit val cfg: Config = v.asInstanceOf[ConfigObject].toConfig
      // need actor system
      val as = config.asOpt[String](AkkaAlias).flatMap(ctx.getOpt[ActorSystem](_)).getOrElse(ctx.get[ActorSystem])

      val cc: ConnectionContext = config.asOpt[String](Protocol).getOrElse("http") match {
        case "http" => ConnectionContext.noEncryption()
        case "https" =>
          val sSLConfig = config.as[SSLConfig](SSLCfg)
          ConnectionContext.https(sSLConfig.sSLContext)
        case x => throw new IllegalArgumentException(s"Unsupported protocol: $x. Can't be parsed to type: ${classOf[RouteRegistry].getName}")
      }
      val data = t(v)
      RouteRegistryImpl(data)(as, cc)
    }

  override def run(ctx: Context) = {
    import scala.concurrent.ExecutionContext.Implicits.global
    val httpConfigs = ctx.registered[RouteRegistry].keySet.map(_.asInstanceOf[RouteRegistryImpl])
    val futures = httpConfigs.map(p => p().transform(p => p, th => RunnableError(s"Can't initialize route ${p}", th)))
    Future.sequence(futures).map { p =>
      ctx
    }
  }

  override def dispose(ctx: Context) = {
    import scala.concurrent.ExecutionContext.Implicits.global
    val futures = bindings.map(p => p.unbind().transform(p => p, th => DisposableError(s"Can't initialize route ${p}", th)))
    Future.sequence(futures).map { p => () }
  }

  override def dependencies(ds: TypeInfoChain): TypeInfoChain = ds.&&[Config].&&[LoggingAdapterFactory].&&[ActorSystem]

  override def publish(ds: TypeInfoChain): TypeInfoChain = ds.&&[RouteRegistry]

  /**
    *
    * @param path
    * @return
    */
  @throws[IllegalArgumentException]
  private def getResource(path: String): InputStream = {

    def fileIsOk(f: File) = f.exists() && f.isFile

    val df = new File(path)
    if (fileIsOk(df)) new FileInputStream(df)
    else {
      // in zip
      val res = this.getClass.getClassLoader.getResource(path)
      if (res != null) this.getClass.getClassLoader.getResourceAsStream(res.toExternalForm)
      else {
        // in user dir
        val d = System.getProperty("user.dir") + File.separator + path
        val df = new File(d)
        if (fileIsOk(df)) {
          new FileInputStream(df)
        } else {
          // in user home
          val d = System.getProperty("user.home") + File.separator + path
          val df = new File(d)
          if (fileIsOk(df)) {
            new FileInputStream(df)
          } else throw new IllegalArgumentException(s"Path: $path doesn't exists")
        }
      }
    }
  }
}