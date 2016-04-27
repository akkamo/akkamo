package com.github.jurajburian.makka.config

import com.github.jurajburian.makka.{Context, Initializable, Module}
import com.typesafe.config.ConfigFactory

/**
	* @author jubu
	*/
class ConfigModule extends Module  with Initializable {
	override def initialize(ctx: Context): Boolean = {
		//TODO better configuration
		val c = ConfigFactory.defaultApplication()
		ctx.register(c)
		true
	}
}
