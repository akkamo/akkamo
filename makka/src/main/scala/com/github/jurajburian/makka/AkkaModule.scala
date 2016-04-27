package com.github.jurajburian.makka

import akka.actor.ActorSystem
import com.typesafe.config.Config

import scala.util.Try

/**
	* Register one or more Actor System
	* {{{
	*   configuration example:
	*   makka.akka = [
	*     { // one block with akka configuration contains several aliases and has name
	*       aliases = ["alias1, "alias2"]
	*       name = "akka-instance1" // name is not mandatory - generated "default"
	*       // standard akka attributes for example:
	*      	loglevel = "DEBUG"
	*      	debug {
	*      	  lifecycle = on
	*      	}
	*      	// ....
	*     },
	*     { // not aliases - only one block allowed
	*       name = "akka-instance2" // name is not mandatory - generated "default"
	*       ....
	*     }
	*   ]
	* }}}
	* in a case when name is generated, only one actor system can be instantiated
	*/
class AkkaModule extends Module with Initializable {

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

	override def initialize(ctx: Context): Boolean = ctx.inject[Config] match {
		case Some(cfg) => {
			import scala.collection.JavaConversions._
			val systems = Try(cfg.getConfigList(AkkaSystemsKey).toList).toOption.fold {
				ctx.register(ActorSystem("default", cfg)) // empty configuration just create default
			} { cfgs =>
				cfgs.map {
					cfg => {
						val as = Try(cfg.getString(Name)).toOption
							.map(name => ActorSystem(name, cfg)).getOrElse(ActorSystem("default", cfg))
						Try(cfg.getStringList(Aliases)).toOption.fold {
							ctx.register(as)
						} { aliases =>
							aliases.map { alias =>
								ctx.register(as, Some(alias))
							}
						}
					}
				}
			}
			true
		}
		case _ => false
	}

	override def toString: String = this.getClass.getSimpleName
}
