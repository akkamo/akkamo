package com.github.jurajburian.makka

/**
 * Provides a signature that given type is Runnable, enables modules to run
 * in given context under given identifier
 *
 * @author jubu
 */
trait Runnable {

	@throws[RunnableError]("If run execution fails")
	def run(ctx:Context):Unit

	/**
	 * Instance of Runnable is registered in the context under this class. <br/>
	 * Override method if want to have different registration key class, for
	 * example an interface instead of concrete class
	 *
	 * @return
	 */
	def rKey() = this.getClass

}

/**
 * Error thrown by Runnable types during run phase
 *
 * @param message
 * @param cause
 */
case class RunnableError(message: String, cause: Throwable = null) extends Error(message, cause)