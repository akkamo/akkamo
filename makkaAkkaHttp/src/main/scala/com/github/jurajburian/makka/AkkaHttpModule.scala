package com.github.jurajburian.makka

import java.security.{KeyStore, SecureRandom}
import javax.net.ssl.{KeyManagerFactory, SSLContext, TrustManagerFactory}

import akka.actor.ActorSystem
import akka.event.LoggingAdapter
import akka.http.scaladsl.Http.ServerBinding
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.{ConnectionContext, Http, HttpsConnectionContext}
import akka.stream.ActorMaterializer
import com.github.jurajburian.makka.RouteRegistry.{HTTP, HTTPS, Protocol}
import com.typesafe.config.Config

import scala.collection.mutable
import scala.concurrent.{Await, Future}

/**
	* For each configuration block, an instance of [[RouteRegistry]] is registered to the
	* ''Makka context'' using its name, aliases (if available) or as default (is specified so).
	* [[RouteRegistry]] serves as main interface for configured ''Akka HTTP'' server, allowing to
	* register own ''Akka HTTP'' routes.
	*
	* @author jubu
	*/
trait RouteRegistry {

	/**
		* Method allowing to register own ''Akka HTTP'' route into configured ''Akka HTTP'' server.
		*
		* @param route ''Akka HTTP'' route to register
		*/
	def register(route: Route): Unit

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
	*   makka.akkaHttp = {
	*     // complete configuration with several name aliases
	*     name1 = {
	*       aliases = ["alias1", "alias2"]
	*       port = 9000 // port, not mandatory
	*       protocol = "http" // http, https, ...
	*       host = "localhost" // host, default localhost
	*       akkaAlias = "alias" // not required, default is used if exists
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
	* the `makka.akkaHttp`, instance of [[RouteRegistry]] is registered to the ''Makka'' context
	* both using its name (e.g. ''name1'') and aliases (e.g. ''alias1'') if provided. Injected
	* instance of [[RouteRegistry]] can be then used to register own Akka HTTP routes.
	*
	* @author jubu
	* @see RouteRegistry
	* @see http://doc.akka.io/docs/akka/current/scala/http/
	*/
class AkkaHttpModule extends Module with Initializable with Runnable with Disposable {

	import config._

	val AkkaHttpKey = "makka.akkaHttp"

	val Protocol = "protocol"

	val Port = "port"

	val Interface = "Interface"

	val AkkaAlias = "akkaAlias"

	val Aliases = "aliases"

	val Default = "default"

	val KeyStorePassword = "keyStorePassword"

	val KeyStoreName = "keyStorePassword"

	val KeyStoreLocation = "keyStorePassword"

	val KeyManagerAlgorithm = "keyManagerAlgorithm"

	val SSLContextAlgorithm = "sSLContext Algorithms"

	val HttpsConnectionContextFactoryClassName = "httpsConnectionContextFactoryClassName"

	type HttpsConnectionContextFactory = (Config)=>HttpsConnectionContext

	private type ServerBindingGetter = () => Future[ServerBinding]

	private var httpConfigs = List.empty[BaseRouteRegistry]

	private var bindings = List.empty[ServerBinding]


	private[AkkaHttpModule] trait BaseRouteRegistry extends RouteRegistry with ServerBindingGetter {
		val routes = mutable.Set.empty[Route]

		override def register(route: Route): Unit = {
			routes += route
		}

		def apply(): Future[ServerBinding] = {
			import akka.http.scaladsl.server.RouteConcatenation
			val rts: Route = RouteConcatenation.concat(routes.toList: _*)
			bind(rts)
		}

		def bind(route: Route): Future[ServerBinding]

		def aliases: List[String]
	}


	private[AkkaHttpModule] case class
	HttpConfig(aliases: List[String], port: Int, interface: String, default: Boolean)
						(implicit as: ActorSystem) extends BaseRouteRegistry {

		def bind(route: Route): Future[ServerBinding] = {
			implicit val am = ActorMaterializer()
			Http().bindAndHandle(route, interface, port)
		}

		val protocol = HTTP
	}

	private[AkkaHttpModule] case class
	HttpsConfig(aliases: List[String], port: Int, interface: String, default: Boolean, ctx: HttpsConnectionContext)
						 (implicit as: ActorSystem) extends BaseRouteRegistry {

		def bind(route: Route): Future[ServerBinding] = {
			implicit val am = ActorMaterializer()
			Http().bindAndHandle(route, interface, port, ctx)
		}

		val protocol = HTTPS
	}


	/**
		* register module mappings
		*
		* @param ctx
		* @return true if initialization is complete.
		*         In case of incomplete initialization system will call this method again.
		*         Incomplete initialization mean That component is not able to find all dependencies.
		*/
	override def initialize(ctx: Context): Boolean = {
		if (ctx.initialized[ConfigModule] && ctx.initialized[LogModule] && ctx.initialized[AkkaModule]) {
			val cfg = ctx.inject[Config]
			val log = ctx.inject[LoggingAdapterFactory].map(_ (this))
			initialize(ctx, cfg.get, log.get)
			true
		} else {
			false
		}
	}

	@throws[InitializationError]
	def initialize(ctx: Context, cfg: Config, log: LoggingAdapter) = {

		// create list of configuration tuples
		val mp = config.blockAsMap(AkkaHttpKey)(cfg)
		if (mp.isEmpty) {
			ctx.register[RouteRegistry](HttpConfig(Nil, 9000, "localhost", true)(
				ctx.inject[ActorSystem].getOrElse(throw InitializationError("Can't find default akka system"))))
		} else {
			val autoDefault = mp.get.size == 1
			httpConfigs = mp.get.toList.map { case (key, cfg) =>
				val system = config.get[String](AkkaAlias, cfg).flatMap(ctx.inject[ActorSystem](_)).orElse(ctx.inject[ActorSystem])
				if (system.isEmpty) {
					throw InitializationError(s"Can't find akka system for http configuration key: $key")
				}
				val protocol = config.get[String](Protocol, cfg).getOrElse("http")
				val port = config.get[Int](Port, cfg).getOrElse(-1)
				val interface = config.get[String](Interface, cfg).getOrElse("localhost")
				val aliases = config.get[List[String]](Aliases, cfg).getOrElse(List.empty[String])
				val default = config.get[Boolean](Default, cfg).getOrElse(autoDefault)
				protocol.toLowerCase match {
					case "http" => HttpConfig(aliases, port, interface, default)(system.get)
					case "https" => try {
						HttpsConfig(aliases, port, interface, default, getHttpsConnectionContext(cfg))(system.get)
					} catch {
						case ie:InitializationError => throw ie
						case th: Throwable => throw InitializationError("Can't initialize https route registry", th)
					}
					case _ => throw InitializationError(s"unknown protocol in route registry, see: $config")
				}
			}
			val combinations = httpConfigs.groupBy(_.interface).map(_._2.groupBy(_.port).size).fold(0)(_ + _)
			if (combinations != httpConfigs.size) {
				throw InitializationError(s"Akka http configuration contains ambiguous combination of port and protocol.")
			}

			for (cfg <- httpConfigs if (cfg.default)) {
				ctx.register[RouteRegistry](cfg)
			}
			for (cfg <- httpConfigs; as <- cfg.aliases) {
				ctx.register[RouteRegistry](cfg, Some(as))
			}
		}
	}


	override def run(ctx: Context): Unit = {
		import scala.concurrent.ExecutionContext.Implicits.global
		import scala.concurrent.duration._
		val futures = httpConfigs.map(p => p().transform(p => p, th => RunnableError(s"Can`t initialize route $p", th)))
		val future = Future.sequence(futures)
		bindings = Await.result(future, 10 seconds)
	}


	@throws[DisposableError]("If dispose execution fails")
	override def dispose(ctx: Context): Unit = {
		import scala.concurrent.ExecutionContext.Implicits.global
		import scala.concurrent.duration._
		val futures = bindings.map(p => p.unbind().transform(p => p, th => DisposableError(s"Can`t initialize route $p", th)))
		val future = Future.sequence(futures)
		Await.result(future, 10 seconds)
	}

	private def getHttpsConnectionContext(cfg:Config): HttpsConnectionContext =
		get[String](HttpsConnectionContextFactoryClassName, cfg)
			.map(getHttpsConnectionContextFormFactory(cfg)).getOrElse(getHttpsConnectionContextFormConfig(cfg))



	private def getHttpsConnectionContextFormFactory(cfg:Config) = (clazzName:String)=> {
		try {
			import scala.reflect.runtime.universe
			val runtimeMirror = universe.runtimeMirror(getClass.getClassLoader)
			val module = runtimeMirror.staticModule(clazzName)
			val companionObj = runtimeMirror.reflectModule(module).instance.asInstanceOf[HttpsConnectionContextFactory]
			companionObj(cfg)
		} catch {
			case th:Throwable => throw new IllegalArgumentException(s"Can't construct HttpsConnectionContext from the factory: $clazzName", th)
		}
	}

	private def getHttpsConnectionContextFormConfig(implicit cfg:Config) = {
		val keyStoreName = config.get[String](KeyStoreName).getOrElse(
			throw InitializationError("Can't find keyStoreName value"))
		val keyStorePassword = config.get[String](KeyStorePassword).getOrElse(
			throw InitializationError("Can't find keyStorePassword value")).toCharArray
		val sslCtx = SSLContext.getInstance("TLS")
		val keyStore = Option(KeyStore.getInstance(keyStoreName)).getOrElse(
			throw InitializationError(s"Can't initialize key store for keyStoreName: $keyStoreName"))
		val keyStoreStream = getClass.getClassLoader.getResourceAsStream(config.get[String](KeyStoreLocation).getOrElse("server.p12"))
		keyStore.load(keyStoreStream, keyStorePassword)
		val keyManagerFactory = KeyManagerFactory.getInstance(get[String](KeyManagerAlgorithm).getOrElse(KeyManagerFactory.getDefaultAlgorithm))
		keyManagerFactory.init(keyStore, keyStorePassword)
		val trustManagerFactory: TrustManagerFactory = TrustManagerFactory.getInstance(keyManagerFactory.getAlgorithm)
		trustManagerFactory.init(keyStore)
		val sslContext: SSLContext = get[String](SSLContextAlgorithm).map(SSLContext.getInstance(_)).getOrElse(SSLContext.getDefault)
		sslContext.init(keyManagerFactory.getKeyManagers, trustManagerFactory.getTrustManagers, SecureRandom.getInstanceStrong)
		ConnectionContext.https(sslContext)
	}
}
