package com.github.jurajburian.makka

import akka.actor.ActorSystem
import com.github.jurajburian.makka
import com.typesafe.config.Config

import scala.concurrent.{Await, Future}
import scala.collection.mutable

/**
	* Register one or more Actor System
	* {{{
	*   configuration example:
	*   makka.akka = {
	*     // one block with akka configuration contains several aliases with the name name
	*     name1 = {
	*       aliases = ["alias1, "alias2"]
	*       // standard akka attributes for example:
	*      	akka{
	*      	  loglevel = "DEBUG"
	*        	debug {
	*        	  lifecycle = on
	*       	}
	*       }
	*      	// ....
	*     },
	*     name2 = { // not aliases - only one block allowed
	*       default = true
	*       ....
	*     }
	*   }
	* }}}
	* In a case when more than one akka configuration exists, one must be denoted as `default` <br/>
	* In case when missing configuration one default Akka system is created with name default.
	*/
class AkkaModule extends Module with Initializable with Disposable {

	/**
		* pointer to array containing set of akka Actor System names in configuration
		*/
	val AkkaSystemsKey = "makka.akka"
	/**
		* in concrete akka config points to array of aliases
		*/
	val Aliases = "aliases"

	/**
		* the name of actor system
		*/
	val Name = "name"

	val actorSystems = mutable.Set.empty[ActorSystem]

	override def initialize(ctx: Context): Boolean = ctx.inject[Config] match {
		case Some(cfg) => {
			import makka.config
			val systems = config.blockAsMap(AkkaSystemsKey)(cfg).fold {
				ctx.register(ActorSystem("default")) // empty configuration just create default
			} { bloks =>
				val bloksWithDefault = if (bloks.size == 1) {
					bloks.map(p => (p._1, p._2, true))
				} else {
					val ret = bloks.map { case (key, cfg) =>
						val default = cfg.hasPath("default") && cfg.getBoolean("default")
						(key, cfg, default)
					}
					val defaultCount = ret.count({ case (_, _, x) => x })
					if (defaultCount != 1) {
						throw new InitializationError(s"In akka module configuration found ${defaultCount} of blocks having default=true. Only one such value is allowed.")
					}
					ret
				}
				bloksWithDefault.map { case (key, cfg, default) =>
					val system = ActorSystem(key, cfg)
					actorSystems += system
					// register under key as name
					ctx.register(system, Some(key))
					// register default
					if (default) {
						ctx.register(system)
					}
					config.getStringList(Aliases)(cfg).map(_.map(name => ctx.register(system, Some(name))))
				}
			}
		}
			true
		case _ => false
	}

	override def toString: String = this.getClass.getSimpleName

	@throws[DisposableError]("If dispose execution fails")
	override def dispose(ctx: Context): Unit = {
		val log = ctx.inject[LoggingAdapterFactory].map(_(getClass)).get
		log.info(s"Terminating Actor systems: $actorSystems")
		import scala.concurrent.ExecutionContext.Implicits.global
		import scala.concurrent.duration._
		val futures  = actorSystems.map(_.terminate)
		val future = Future.sequence(futures)
		Await.result(future, 10 seconds)
	}
}
