package com.github.jurajburian.makka

/**
	* Trait indicating that the module extending this requires to perform additional actions before
	* the ''Makka'' shutdown. This is the very last stage of the ''Makka'' module lifecycle and in
	* this stage, module should close all opened connection, files and get prepared for shutdown.
	*
	* @author jubu
	*/
trait Disposable {

	/**
		* Method the module extending this trait must implement, all logic performed by the module
		* before ''Makka'' system shutdown should be here. As an input parameter, ''Makka'' context is
		* provided.
		*
		* @param ctx ''Makka'' context
		* @throws DisposableError thrown in case of serious unrecoverable error, occured during the
		*                         dispose stage
		*/
	@throws[DisposableError]("If serious unrecoverable problem during dispose stage occurs")
	def dispose(ctx:Context):Unit

}

/**
	* Error to be thrown in case of serious unrecoverable error, occured during the dispose stage.
	*
	* @param message error message
	* @param cause   optional value of cause
	*/
case class DisposableError(message:String, cause:Throwable = null) extends Error(message, cause)