package com.github.jurajburian.makka


/**
	*
	* @author jubu
	*/
trait Initializable {
	/**
		*
		* @param ctx
		* @return true if initialization is complete.
		*         In case of incomplete initialization system will call this method again.
		*         Incomplete initialization mean That component is not able to find all dependencies.
		*/
	@throws[InitializationError]("If initialization can't be finished")
	def initialize(ctx:Context):Boolean
}

/**
	*
	* @param message
	* @param cause
	*/
case class InitializableError(message: String, cause: Throwable = null) extends Error(message, cause)

