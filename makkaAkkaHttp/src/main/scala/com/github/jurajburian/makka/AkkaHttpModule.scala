package com.github.jurajburian.makka

import akka.actor.ActorSystem
import akka.event.LoggingAdapter
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import com.typesafe.config.Config

import scala.collection.mutable
import scala.util.Try


/**
	* @author jubu
	*/
trait HttpRegister {
	def register(route:Route):Unit
}


/**
	* {{{
	*   makka.akkaHttp = [\
	*     // complete configuration with several name aliases
	*     name1 = {
	*       aliases = ["alias1", "alias2"]
	*       port = 9000 // unique port, not mandatory
	*       protocol = "http" // http, https, ...
	*       akkaAlias = "alias" // not required, default is used if exists
	*     },
	*     // configuration registered as default (only one instance is allowed)
	*     name2 = {
	*      protocol = "http" // http, https, ...
	*     }
	*   ]
	* }}}
	* if section `makka.akkaHttp` is missing, then default configuration is generated on port 9000 with http protocol
	* @author jubu
	*/
class AkkaHttpModule extends Module with Initializable with Runnable {

	val AkkaHttpKey = "makka.akkaHttp"

	private case class HttpConfig(aliases:List[String], port:Int, protocol:String)(implicit as:ActorSystem) extends HttpRegister {
		val routes = mutable.Set.empty[Route]

		override def register(route: Route): Unit = {
			routes += route
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
		val cfg = ctx.inject[Config]
		val log = ctx.inject[LoggingAdapterFactory].map(_(this))
		if(cfg.isDefined && log.isDefined) {
			initialize(ctx, cfg.get, log.get)
		} else {
			false
		}
	}

	def initialize(ctx: Context, cfg:Config, log:LoggingAdapter): Boolean = {
		import scala.collection.JavaConversions._
		Try(cfg.getConfigList(AkkaHttpKey).toList).toOption.fold {

			false
		} {p=>
			false
		}
	}

	override def run(): Unit = {

	}
}

