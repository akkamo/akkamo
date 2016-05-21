package com.github.jurajburian.makka

import com.typesafe.config.ConfigFactory

/**
 * @author jubu
 */
class ConfigModule extends Module with Initializable {

  /** Initializes configuration module into provided context
   *
   * @param ctx muttable context
   */
  override def initialize(ctx:Context):Boolean = {
		//TODO better configuration
    //TODO what it this configuration fails, no case and return false?
		val c = ConfigFactory.defaultApplication()
		ctx.register(c)
		true
	}

  //FIXME maybe unneccessary, already overriden with same code in Module type
	override def toString:String = this.getClass.getSimpleName

}
