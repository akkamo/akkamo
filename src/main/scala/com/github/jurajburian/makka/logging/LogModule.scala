package com.github.jurajburian.makka.logging

import akka.actor.ActorSystem
import com.github.jurajburian.makka.{Context, Initializable, Module}
import com.typesafe.config.Config

/**
	* @author jubu
	*/
class LogModule extends Module with Initializable {
	override def initialize(ctx: Context): Boolean = ctx.inject[ActorSystem] match {

	}

}
