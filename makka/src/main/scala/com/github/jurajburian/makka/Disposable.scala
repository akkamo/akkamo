package com.github.jurajburian.makka

/**
	* @author jubu
	*/
trait Disposable {

	@throws[DisposableError]("If dispose execution fails")
	def dispose(ctx:Context):Unit
}


/**
	*
	* @param message
	* @param cause
	*/
case class DisposableError(message: String, cause: Throwable = null) extends Error(message, cause)