package com.github.jurajburian.makka

/**
	* @author jubu
	*/
trait Runnable {

	@throws[RunnableError]("If run execution fails")
	def run(ctx:Context):Unit
}


/**
	*
	* @param message
	* @param cause
	*/
case class RunnableError(message: String, cause: Throwable = null) extends Error(message, cause)