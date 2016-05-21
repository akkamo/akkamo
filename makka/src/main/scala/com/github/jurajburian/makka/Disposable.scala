package com.github.jurajburian.makka

/** The last module lifecycle signature
 *
 * @author jubu
 */
trait Disposable {

  //TODO better message
	@throws[DisposableError]("If dispose execution fails")
	def dispose(ctx:Context):Unit

}

/**
 *
 * @param message
 * @param cause
 */
case class DisposableError(message:String, cause:Throwable = null) extends Error(message, cause)