package com.github.jurajburian.makka

/**
	* Trait indicating that the module extending this requires to perform initialization during Makka
	* startup. Initialization is the very first stage of ''Makka'' module lifecycle and in this stage,
	* module should check all its required dependencies and/or register its own provided functionality
	* into context.
	*
	* @author jubu
	*/
trait Initializable {

	/**
		* Method the module extending this trait must implement, all module initialization logic should
		* be performed here. ''Makka'' context is given as a parameter, allowing access to all required
		* dependencies and allows to register module's own functionality. The boolean return value
		* should determine whether the module has been properly initialized or not (e.g. not all
		* dependencies are initialized yet)
		*
		* @param ctx ''Makka'' context
		* @return `true` if the module has been properly initialized
		* @throws InitializationError thrown when severe error occurs during the initialization
		*                             and there is no option to recover such state
		*/
	@throws[InitializationError]("If initialization can't be finished")
	def initialize(ctx:Context):Boolean

	/**
		* Instance of [[Initializable]] is registered into the ''Makka'' context by default under
		* this module class. Override this method in order to achieve different registration key
		* class, for example an interface instead of concrete implementation.
		*
		* @return registration key class
		*/
	def iKey() = this.getClass

}

/**
	* Error to be thrown during the initialization of module in case of serious unrecoverable problem.
	*
	* @param message error message
	* @param cause   optional value of cause
	*/
case class InitializableError(message:String, cause:Throwable = null) extends Error(message, cause)

