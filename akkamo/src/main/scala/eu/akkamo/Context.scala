package eu.akkamo

import scala.collection.{Map, Set}

/**
  * A mutable context dispatched during all phases in module lifecycle
  *
  * @author jubu
  */
trait Context {

  import scala.reflect.ClassTag

  /**
    * inject registered service into this context
    *
    * @param ct
    * @tparam T
    * @return implementation of interface `T` if initialized
    */
  def inject[T](implicit ct: ClassTag[T]): Option[T]

  /**
    * inject registered service into this context
    *
    * @param key    additional mapping identifier
    * @param strict - if true, only named instance is returned if false then
    *               when nothing found under key then default instance is
    *               returned if it exists and is initialized
    * @param ct     class tag evidence
    * @tparam T
    * @return implementation of interface `T` if initialized
    */
  def inject[T](key: String, strict: Boolean = false)(implicit ct: ClassTag[T]): Option[T]

  /**
    * get mapping from services to set of aliases
    *
    * @param ct
    * @tparam T
    * @return map of all services of type `T` to set of aliases
    */
  def registered[T](implicit ct: ClassTag[T]): Map[T, Set[String]]


  /**
    * register bean
    *
    * @param value
    * @param key
    * @param ct class tag evidence
    * @tparam T
    */
  def register[T <: AnyRef](value: T, key: Option[String] = None)(implicit ct: ClassTag[T]): Context

}
