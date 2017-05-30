package eu.akkamo.m.config

import com.typesafe.config.ConfigValue


/**
  * ''Typeclass'' config ''transformer'', allowing to extract value of type `T` for the given configuration value.
  *
  * @tparam T type of the extracted value
  */
trait Transformer[T] {

  /**
    *
    * @param v instance of [[com.typesafe.config.ConfigValue]] or `null`
    * @return instance created from a `ConfigValue` or throws NullPointerException
    * @throws throws Exception if `v` can't be converted to `T`
    */
  @throws[Throwable]
  def apply(v: ConfigValue): T
}
