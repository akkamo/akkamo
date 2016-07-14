package eu.akkamo

import scala.collection.{Map, Set}


/**
  * The ''Registry'' provides functionality to register a holder object to the ''Akkamo'' context,
  * which value can be changed later. Note that this structure is immutable, hence the value
  * replacement will yield new instance of [[Registry]]. The value is not intended to be changed
  * directly, but using the `registerIn` method.
  *
  * For further clarification, let's introduce simple example. Let the `RouteRegistry` be fictional (see. akkamoAkkaHttp project for real example)
  * implementation of `Registry`, representing the collection of all registered REST HTTP
  * endpoints, allowing any other module to register own `Route`, representing particilar
  * HTTP route (e.g. `GET /foo/bar`).
  *
  * {{{
  *   // register the custom RouteRegistry into the provided HttpRegistry
  *   ctx.registerIn[RouteRegistry, Route](instanceOfRoute)
  * }}}
  *
  * For more details about creating and registering custom registry, please refer the official
  * ''Akkamo'' online documentation here: [[http://akkamo.eu]]
  *
  * @tparam T type of held value
  */
trait Registry[T] {

  /**
    * Creates new instance of particular [[Registry]] implementation, with the new given value.
    *
    * @param p new value
    * @return new instance of particular [[Registry]] implementation
    */
  def copyWith(p: T): this.type
}


/**
  * A mutable context dispatched during all phases in module lifecycle
  *
  * @author jubu
  */
trait Context {

  import scala.reflect.ClassTag

  /**
    * Injects service already registered as an ''default'' service in ''Akkamo'' context.
    *
    * @param ct ''Akkamo'' context
    * @tparam T type of the service
    * @return implementation of interface `T` encapsulated in `Some` if exists else `None`
    */
  def inject[T](implicit ct: ClassTag[T]): Option[T]

  /**
    * Injects service already registered in ''Akkamo'' context, using the ''key'' using which the
    * service has been registered. By default, if no service is found with this key, ''default''
    * service is returned (if available). To disable this behavior, set the `strict` parameter to
    * `false`.
    *
    * @param key    additional mapping identifier
    * @param strict if `true`, only named instance is returned if false then when nothing found
    *               under key then default instance is returned if it exists and is initialized
    * @param ct     class tag evidence
    * @tparam T type of the service
    * @return implementation of interface `T` encapsulated in `Some` if exists else `None`
    */
  def inject[T](key: String, strict: Boolean = false)(implicit ct: ClassTag[T]): Option[T]


  /**
    * Get service already registered as an ''default'' service in ''Akkamo'' context.
    *
    * @param ct ''Akkamo'' context
    * @tparam T type of the service
    * @throws ContextError if bean not exists
    * @return implementation of interface `T`
    */
  @throws[ContextError]
  def get[T](implicit ct: ClassTag[T]): T = inject[T]
    .getOrElse(throw new ContextError(s"Can't find registered bean of type: ${ct.runtimeClass.getName}"))

  /**
    * Get service already registered in ''Akkamo'' context, using the ''key'' using which the
    * service has been registered. By default, if no service is found with this key, ''default''
    * service is returned (if available). To disable this behavior, set the `strict` parameter to
    * `false`.
    *
    * @param key    additional mapping identifier
    * @param strict if `true`, only named instance is returned if false then when nothing found
    *               under key then default instance is returned if it exists and is initialized
    * @param ct     class tag evidence
    * @tparam T type of the service
    * @throws ContextError if bean not exists
    * @return implementation of interface `T`
    */
  @throws[ContextError]
  def get[T](key: String, strict: Boolean = false)(implicit ct: ClassTag[T]): T = inject[T](key, strict)
    .getOrElse(throw new ContextError(s"Can't find registered bean under key: $key of type: ${ct.runtimeClass.getName}"))



  /**
    * register bean `x` to previously `registered` bean of type `T`
    *
    * @param x   registered instance
    * @param key mapping identifier of previously registered instance of type `T`
    * @param ct  class tag evidence
    * @tparam T type of `registered` registered object
    * @tparam X type of instance going to be registered
    * @return updated instance of `Context`
    */
  @throws[ContextError]
  def registerIn[T <: Registry[X], X](x: X, key: Option[String] = None)(implicit ct: ClassTag[T]): Context


  /**
    * register bean `x` to previously `registered` bean of type `T`
    *
    * @param x   registered instance
    * @param key mapping identifier of previously registered instance of type `T`
    * @param ct  class tag evidence
    * @tparam T type of `registered` registered object
    * @tparam X type of instance going to be registered
    * @return updated instance of `Context`
    */
  @throws[ContextError]
  def registerIn[T <: Registry[X], X](x: X, key: String)(implicit ct: ClassTag[T]): Context = registerIn[T, X](x, Some(key))

  /**
    * register bean
    *
    * @param value
    * @param key
    * @param ct class tag evidence
    * @tparam T
    * @return updated instance of `Context`
    */
  def register[T <: AnyRef](value: T, key: Option[String] = None)(implicit ct: ClassTag[T]): Context


  /**
    * register bean
    *
    * @param value
    * @param key
    * @param ct class tag evidence
    * @tparam T
    * @return updated instance of `Context`
    */
  def register[T <: AnyRef](value: T, key: String)(implicit ct: ClassTag[T]): Context = register[T](value, Some(key))


  /**
    * get mapping from services to set of aliases
    *
    * @param ct
    * @tparam T
    * @return map of all services of type `T` to set of aliases
    */
  def registered[T <: AnyRef](implicit ct: ClassTag[T]): Map[T, Set[String]]
}


/**
  *
  * @param message
  * @param cause
  */
case class ContextError(message: String, cause: Throwable = null) extends Error(message, cause)
