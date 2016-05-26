package com.github.jurajburian.makka

import akka.actor.ActorSystem
import com.typesafe.config.Config

import scala.concurrent.{Await, Future}
import scala.collection.mutable
import scala.util.{Failure, Success, Try}

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

	/**
	 * Initializes the module into provided mutable context, blocking
	 */
	override def initialize(ctx:Context):Boolean = ctx.inject[Config] match {
		case Some(cfg) => {
			val systems = config.blockAsMap(AkkaSystemsKey)(cfg).fold {
				// empty configuration just create default
				Try(ctx.register(ActorSystem("default"))) match {
					case Success(_)=>
					case Failure(th)=> throw InitializableError("Can't initialize default Akka system", th)
				}
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
						throw new InitializableError(s"In akka module configuration found ${defaultCount} of blocks having default=true. Only one such value is allowed.")
					}
					ret
				}
				bloksWithDefault.foreach { case (key, cfg, default) =>
					Try {
						import config._
						val system = ActorSystem(key, cfg)
						actorSystems += system
						// register under key as name
						ctx.register(system, Some(key))
						// register default
						if (default) {
							ctx.register(system)
						}
						config.get[List[String]](Aliases, cfg).map(_.map(name => ctx.register(system, Some(name))))
					} match {
						case Success(_)=>
						case Failure(th)=> throw InitializableError(s"Can't initialize Akka system defined by: $key", th)
					}
				}
			}
		}
		true
		case _ => false
	}

	@throws[DisposableError]("If dispose execution fails")
	override def dispose(ctx: Context): Unit = {
		val log = ctx.inject[LoggingAdapterFactory].map(_(getClass)).get
		log.info(s"Terminating Actor systems: $actorSystems")

		import scala.concurrent.ExecutionContext.Implicits.global
		import scala.concurrent.duration._

		val futures  = actorSystems.map(p=>p.terminate.transform(p=>p, th=>DisposableError(s"Can`t initialize route $p", th)))
		val future = Future.sequence(futures)
		Await.result(future, 10 seconds)
	}

}
