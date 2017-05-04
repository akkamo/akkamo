package eu.akkamo

/**
  * Trait indicating that the module extending this requires to perform initialization during Akkamo
  * startup. Initialization is the very first stage of ''Akkamo'' module lifecycle and in this stage,
  * module should check all its required dependencies and/or register its own provided functionality
  * into context.
  *
  * @author jubu
  */
trait Initializable {
  /**
    * Method the module extending this trait must implement, all module initialization logic should
    * be performed here. ''Akkamo'' context is given as a parameter, allowing access to all required
    * dependencies and allows to register module's own functionality. The boolean return value
    * should determine whether the module has been properly initialized or not (e.g. not all
    * dependencies are initialized yet)
    *
    * @param ctx ''Akkamo'' context
    * @return instance of Res that contains (new if modified) instance of [[eu.akkamo.Context]] or
    *         exception packed in ``Try``
    */
  def initialize(ctx: Context): Res[Context]

}

/**
  * Recommended Error to be thrown inside [[eu.akkamo.Initializable#initialize]] method if serious
  * unrecoverable problem occurs.
  *
  * @param message error message
  * @param cause   optional value of cause
  */
case class InitializableError(message: String, cause: Throwable = null) extends AkkamoError(message, cause)
