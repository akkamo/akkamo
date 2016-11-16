package eu.akkamo


/**
  * Simple factory, providing `LoggingAdapter` instance for specified category (e.g. module name,
  * selected class). This factory is registered by the [[LogModule]] into the ''Akkamo'' context.
  *
  * @author jubu
  */
trait LoggingAdapterFactory {

  /**
    * Returns instance of `LoggingAdapter` for specified category (e.g. module name,
    * selected class).
    *
    * @param category category for which the `LoggingAdapter` will be returned
    * @tparam T type of the category class
    * @return instance of `LoggingAdapter`
    */
  def apply[T](category: Class[T]): LoggingAdapter

  /**
    * Returns instance of `LoggingAdapter` for specified category (e.g. module name,
    * selected class).
    *
    * @param category category for which the `LoggingAdapter` will be returned
    * @return instance of `LoggingAdapter`
    */
  def apply(category: AnyRef): LoggingAdapter
}