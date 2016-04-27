package com.github.jurajburian.makka

import akka.actor.ActorSystem
import com.typesafe.config.Config

import scala.util.Try

/**
	* Register one or more Actor System
	*
	*/
class AkkaModule extends Module with Initializable {

	/**
		* pointer to array containing set of akka Actor System names in configuration
		*/
	val AkkaSystemsKey = "akka.systems"

	override def initialize(ctx: Context): Boolean = ctx.inject[Config] match {
		case Some(cfg) => {
			import scala.collection.JavaConversions._
			val systems = Try(cfg.getStringList(AkkaSystemsKey).toList).toOption.fold {
				ctx.register(ActorSystem("default", cfg))
			} { names =>
				names.map {
					name => {
						ctx.register(ActorSystem(name, cfg.getConfig("name")), Some(name))
					}
				}
			}
			true
		}
		case _ => false
	}

	override def toString: String = this.getClass.getSimpleName
}
