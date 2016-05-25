package com.github.jurajburian.makka

import javax.net.ssl.SSLContext

import akka.actor.ActorSystem
import akka.event.LoggingAdapter
import akka.http.scaladsl.{ConnectionContext, Http}
import akka.http.scaladsl.Http.ServerBinding
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import com.github.jurajburian.makka.RouteRegistry.{HTTP, Protocol}
import com.typesafe.config.Config

import scala.collection.mutable
import scala.concurrent.{Await, Future}
import scala.util.Try


/**
	* @author jubu
	*/
trait RouteRegistry {

	def register(route: Route): Unit

	def port: Int

	def interface: String

	def protocol: Protocol

	def default: Boolean
}

object RouteRegistry {

	sealed trait Protocol {
		def name = toString;
	}

	case object HTTP extends Protocol {
		override def toString: String = "http"
	}

	case object HTTPS extends Protocol {
		override def toString: String = "https"
	}

}

/**
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
	* if section `makka.akkaHttp` is missing, then default configuration is generated on port 9000 with http protocol
	*
	* @author jubu
	*/
class AkkaHttpModule extends Module with Initializable with Runnable with Disposable {

	val AkkaHttpKey = "makka.akkaHttp"

	val Protocol = "protocol"

	val Port = "port"

	val Interface = "Interface"

	val AkkaAlias = "akkaAlias"

	val Aliases = "aliases"

	val Default = "default"

	type BINDABLE =  ()=>Future[ServerBinding]

	private var httpConfigs = List.empty[BaseRouteRegistry]
	private var bindings = List.empty[ServerBinding]


	private[AkkaHttpModule]  trait BaseRouteRegistry extends RouteRegistry with BINDABLE {
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


	private[AkkaHttpModule] case class HttpConfig(aliases: List[String], port: Int, interface: String, default: Boolean)(implicit as: ActorSystem) extends BaseRouteRegistry {

		def bind(route: Route): Future[ServerBinding] = {
			implicit val am = ActorMaterializer()
			Http().bindAndHandle(route, interface, port)
		}
		val protocol = HTTP
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
		import config._

		// create list of configuration tuples
		val mp = config.blockAsMap(AkkaHttpKey)(cfg)
		if (mp.isEmpty) {
			ctx.register[RouteRegistry](HttpConfig(Nil, 9000, "localhost", true)(
				ctx.inject[ActorSystem].getOrElse(throw InitializationError("Can't find default akka system"))))
		} else {
			val autoDefault = mp.get.size == 1
			val httpCfgs = mp.get.toList.map { case (key, cfg) =>
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
					case "http" => {
						val ret = HttpConfig(aliases, port, interface, default)(system.get)
						ret
					}
					case "https" => try {
						val sslCtx = SSLContext.getInstance("TLS")
						//val context = ConnectionContext.https(sslCtx, )
						throw InitializationError("Https is not supported ")
					} catch {
						case th:Throwable => throw InitializationError("Can't initialize https route registry", th)
					}
					case _ => {
						throw InitializationError(s"unknown protocol in route registry, see: $config")
					}
				}
				(key :: aliases, port, interface, protocol, system.get, default, config)
			}
			val combinations = httpCfgs.groupBy(_._3).map(_._2.groupBy(_._2).size).fold(0)(_ + _)
			if (combinations != httpCfgs.size) {
				throw InitializationError(s"Akka http configuration contains ambiguous combination of port and protocol.")
			}
			httpConfigs = httpCfgs.map { case rr@(aliases, port, interface, protocol, system, default, config) =>

				protocol.toLowerCase match {
					case "http" => {
						log.info(s"route registry: $rr created")
						HttpConfig(aliases, port, interface, default)(system)
					}
					// TODO HTTPS
					case _ => {
						throw InitializationError(s"unknown protocol in route registry: $rr definition")
					}
				}
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

}
