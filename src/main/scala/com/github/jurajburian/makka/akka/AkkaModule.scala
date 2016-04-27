package com.github.jurajburian.makka.akka

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, Materializer}
import com.github.jurajburian.makka.{Context, Initializable, Module}
import com.typesafe.config.Config

import scala.util.Try

/**
	* Register one or more Actor System
	*
	* @author jubu
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
