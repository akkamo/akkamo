package eu.akkamo

/**
  * Base Error
  *
  * @param message error message
  * @param cause   optional value of cause
  */

class AkkamoError(message: String, cause: Throwable = null) extends Error(message, cause)
