package com.github.jurajburian.makka.akka

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, Materializer}
import com.github.jurajburian.makka.{Context, Initializable, Module}
import com.typesafe.config.Config

import scala.util.Try

/**
	* Register one or more Actor Systems & Matriealizer
	* @author jubu
	*/
class AkkaModule extends Module with Initializable {
	override def initialize(ctx: Context): Boolean = ctx.inject[Config] match {
		case Some(x) => {
			import scala.collection.JavaConversions._
			val systems = Try(x.getStringList("akka.systems").toList).toOption.fold {
				List(ActorSystem("default", x))
			} { names =>
				names.map {
					name =>
						ActorSystem(name, x.getConfig("name"))
				}
			}
			systems.map{p=>
				implicit val ctc = p
				ctx.register(p, Some(p.name))
				val mat = ActorMaterializer()
				ctx.register(mat, Some(p.name))
			}
			ActorMaterializer
			true
		}
		case _ => false


	}
}
