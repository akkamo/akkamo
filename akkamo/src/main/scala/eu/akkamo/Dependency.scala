package eu.akkamo

import scala.reflect.ClassTag

/**
  * Represents single module dependency. Provides convenient methods for chaining dependencies.
  *
  * @author jubu
  */
trait Dependency {

  /**
    * Convenient method allowing to chain multiple module dependencies. For usage details, see
    * [[eu.akkamo.Module!.dependencies]].
    *
    * @tparam T type of dependency
    * @return chained dependencies
    */
  def &&[T:ClassTag]: Dependency

  def res: Boolean
}
