package com.github.jurajburian.makka

import akka.actor.ActorSystem
import akka.event.LoggingAdapter
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import com.typesafe.config.Config

import scala.collection.mutable
import scala.concurrent.{Await, Future}


/**
	* @author jubu
	*/
trait RouteRegister {
	def register(route: Route): Unit
}


/**
	* {{{
	*   makka.akkaHttp = {
	*     // complete configuration with several name aliases
	*     name1 = {
	*       aliases = ["alias1", "alias2"]
	*       port = 9000 // unique port, not mandatory
	*       protocol = "http" // http, https, ...
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
class AkkaHttpModule extends Module with Initializable with Runnable {

	val AkkaHttpKey = "makka.akkaHttp"

	val Protocol = "protocol"

	val Port = "port"

	val AkkaAlias = "akkaAlias"

	val Aliases = "aliases"

	private var httpConfigs = List.empty[HttpConfig]
	private var bindings = List.empty[ServerBinding]

	private[AkkaHttpModule] case class HttpConfig(aliases: List[String], port: Int, protocol: String)(implicit as: ActorSystem) extends RouteRegister {
		val routes = mutable.Set.empty[Route]

		override def register(route: Route): Unit = {
			routes += route
		}

		def run(): Future[ServerBinding] = {
			import akka.http.scaladsl.server.RouteConcatenation
			implicit val am = ActorMaterializer()
			Http().bindAndHandle(RouteConcatenation.concat(routes.toList: _*), protocol, port)
		}
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
			// TODO if cfg or log is None - throw exception
			initialize(ctx, cfg.get, log.get)
		} else {
			false
		}
	}

	@throws[InitializationError]
	def initialize(ctx: Context, cfg: Config, log: LoggingAdapter): Boolean = {
		// create list of configuration tuples
		val mp = config.blockAsMap(AkkaHttpKey)(cfg)
		if (mp.isEmpty) {
			throw InitializationError("Missing Akka http configuration.")
		}
		val httpCfgs = mp.get.toList.map { case (key, cfg) =>
			val system = config.getString(AkkaAlias)(cfg).map(ctx.inject[ActorSystem](_)).getOrElse(ctx.inject[ActorSystem])
			if (system.isEmpty) {
				throw InitializationError(s"Can't find akka system for http configuration: $cfg")
			}
			val protocol = config.getString(Protocol)(cfg).getOrElse("http")
			val port = config.getInt(Port)(cfg).getOrElse(-1)
			val aliases = config.getStringList(Aliases)(cfg).getOrElse(List.empty[String])
			(key :: aliases, port, protocol, system.get, cfg)
		}
		val combinations = httpCfgs.groupBy(_._3).map(_._2.groupBy(_._2).size).fold(0)(_ + _)
		if (combinations != httpCfgs.size) {
			throw InitializationError(s"Akka http configuration contains ambiguous combination of port and protocol.")
		}
		httpConfigs = httpCfgs.map { case (aliases, port, protocol, system, _) => HttpConfig(aliases, port, protocol)(system) }
		for(cfg<-httpConfigs; as<-cfg.aliases) {
			ctx.register[RouteRegister](cfg,Some(as))
		}
		true
	}


	override def run(ctx: Context): Unit = {
		import scala.concurrent.ExecutionContext.Implicits.global
		import scala.concurrent.duration._
		val futures = httpConfigs.map(p => p.run())
		val f = Future.sequence(futures)
		bindings = Await.result(f, 10 seconds)
	}
}

