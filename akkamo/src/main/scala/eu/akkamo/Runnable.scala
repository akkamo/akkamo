package eu.akkamo

/**
	* Trait indicating that the module extending this requires to perform some actions after the
	* initialization stage (i.e. in time when all modules are initialized).
	*
	* @author jubu
	*/
trait Runnable {

	/**
		* Method the module extending this trait must implement, contains module logic to be performed
		* after the initialization stage (i.e. when all modules are initialized). As an input parameter,
		* the ''Akkamo'' context is given.
		*
		* @param ctx ''Akkamo'' context
		* @throws RunnableError thrown in case of serious unrecoverable error during the run stage
		*/
	@throws[RunnableError]("If run execution fails")
	def run(ctx:Context):Unit

	/**
		* Instance of [[Runnable]] is registered into the ''Akkamo'' context by default under
		* this module class. Override this method in order to achieve different registration key
		* class, for example an interface instead of concrete implementation.
		*
		* @return registration key class
		*/
	def rKey() = this.getClass

}

/**
	* Error to be thrown in case of serious unrecoverable error during the run stage.
	*
	* @param message error message
	* @param cause   optional value of cause
	*/
case class RunnableError(message: String, cause: Throwable = null) extends Error(message, cause)