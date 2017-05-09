package eu.akkamo.config

import com.typesafe.config.ConfigValue

/**
  * ''Typeclass'' config ''transformer'', allowing to extract value of type `T` for the given
  * config key and configuration instance.
  *
  * @tparam T type of the extracted value
  */
trait Transformer[T] {
  def apply(v:ConfigValue):T
}
