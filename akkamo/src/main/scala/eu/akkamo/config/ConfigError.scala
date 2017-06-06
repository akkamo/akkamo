package eu.akkamo.config

import eu.akkamo.AkkamoError

/**
  * Error thrown when value under given alias does not exists
  *
  * @param message detail message
  * @param cause   optional error cause
  */
case class ConfigError(message: String, cause: Throwable = null) extends AkkamoError(message, cause)
