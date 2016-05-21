package com.github.jurajburian.makka

/** The first module lifecycle signature
 *
 * @author jubu
 */
trait Initializable {

	/**
	 *
	 * @param ctx
	 * @return true if initialization is complete.
	 *         In case of incomplete initialization system will call this method
	 *         again. <br/> Incomplete initialization mean That component is not
	 *         able to find all required dependencies.
	 */
	@throws[InitializationError]("If initialization can't be finished")
	def initialize(ctx:Context):Boolean

	/**
	 * Instance of initalizable is registered in the context under this "class"
	 * key. <br/> Override method if want to have different registration key
	 * class, for example an interface instead of concrete class
	 *
	 * @return
	 */
	def iKey() = this.getClass

}

/**
 *
 * @param message
 * @param cause
 */
case class InitializableError(message:String, cause:Throwable = null) extends Error(message, cause)

