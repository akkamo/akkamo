package eu.akkamo

/**
  * Trait indicating that the module extending this requires to perform additional actions before
  * the ''Akkamo'' shutdown. This is the very last stage of the ''Akkamo'' module lifecycle and in
  * this stage, module should close all opened connection, files and get prepared for shutdown.
  *
  * @author jubu
  */
trait Disposable {

  /**
    * Method the module extending this trait must implement, all logic performed by the module
    * before ''Akkamo'' system shutdown should be here. As an input parameter, ''Akkamo'' context is
    * provided.
    *
    * @param ctx ''Akkamo'' context
    * @return instance of Res
    */
  def dispose(ctx: Context): Res[Unit]
}

/**
  * Error to be thrown in case of serious unrecoverable error, occured during the dispose stage.
  *
  * @param message error message
  * @param cause   optional value of cause
  */
case class DisposableError(message: String, cause: Throwable = null) extends Error(message, cause)
