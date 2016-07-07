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
    * @return instance of Res that contains (new if modified) instance of [[eu.akkamo.Context]] or exception packed in ``Try``
    */
  def run(ctx: Context): Res[Context]

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
  * Recommended Error to be thrown inside [[eu.akkamo.Runnable#run]] method if serious unrecoverable problem occurs.
  *
  * @param message error message
  * @param cause   optional value of cause
  */
case class RunnableError(message: String, cause: Throwable = null) extends Error(message, cause)