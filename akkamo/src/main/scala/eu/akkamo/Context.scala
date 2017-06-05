package eu.akkamo

import scala.collection.{Map, Set}


/**
  * The ''Registry'' provides functionality to register a holder object to the ''Akkamo'' context,
  * which value can be changed later. Note that this structure is immutable, hence the value
  * replacement will yield new instance of [[Registry]]. The value is not intended to be changed
  * directly, but using the `registerIn` method.
  *
  * For further clarification, let's introduce simple example. Let the `RouteRegistry` be fictional
  * (see. akkamo AkkaHttp project for real example) implementation of `Registry`, representing the
  * collection of all registered REST HTTP endpoints, allowing any other module to register own
  * `Route`, representing particular HTTP route (e.g. `GET /foo/bar`).
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
  * ''Akkamo'' context represents the cornerstone of the ''Akkamo'' platform, as it is the place
  * where different modules can register its own services provided for other modules (using
  * one of `registerIn` methods) or can inject services already registered by other modules
  * (using one of the `inject` method). Please note that the [[Context]] is immutable and any
  * update performed (e.g. registering service) will yield new, updated instance.
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
    .getOrElse(throw ContextError(s"Can't find registered bean of type: ${ct.runtimeClass.getName}"))

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
    .getOrElse(throw ContextError(s"Can't find registered bean under key: ${key} of type: ${ct.runtimeClass.getName}"))


  /**
    * Registers value into service, specified by its type and optional ''key'', previously
    * registered to the ''Akkamo'' context. In fact, this serves as a shorthand for manually
    * injecting the desired service from the context, and then registering the selected value into
    * it.
    *
    * @param x   value to register
    * @param key optional key, under which the service was previously registered into the context
    * @param ct  class tag evidence
    * @tparam T type of previously registered service
    * @tparam X type of value to be registered into the service
    * @throws ContextError if operation cannot be completed for various reasons
    * @return updated instance of immutable [[Context]]
    */
  @throws[ContextError]
  def registerIn[T <: Registry[X], X](x: X, key: Option[String] = None)(implicit ct: ClassTag[T]): Context


  /**
    * Registers value into service, specified by its type and ''key'', previously registered to the
    * ''Akkamo'' context. In fact, this serves as a shorthand for manually injecting the desired
    * service from the context, and then registering the selected value into it.
    *
    * @param x   value to register
    * @param key optional key, under which the service was previously registered into the context
    * @param ct  class tag evidence
    * @tparam T type of previously registered service
    * @tparam X type of value to be registered into the service
    * @throws ContextError if operation cannot be completed for various reasons
    * @return updated instance of immutable [[Context]]
    */
  @throws[ContextError]
  def registerIn[T <: Registry[X], X](x: X, key: String)(implicit ct: ClassTag[T]): Context = registerIn[T, X](x, Some(key))


  /**
    * Registers service to the ''Akkamo'' context, such service is then available to any other
    * module. This method allows to register service with optional
    * ''key'', which is primarily used to disambiguate between multiple registered services of same
    * type and must be then specified when injecting the service. Please note that because the
    * [[Context]] is immutable, new updated instance will be returned after calling this method.
    *
    * @param value service to register
    * @param key   optional key
    * @param ct    class tag evidence
    * @tparam T type of the service
    * @return new updated instance of [[Context]]
    */
  def register[T <: AnyRef](value: T, key: Option[String] = None)(implicit ct: ClassTag[T]): Context


  /**
    * Registers service to the ''Akkamo'' context, such service is then available to any other
    * module. This method allows to register service with ''key'',
    * which is primarily used to disambiguate between multiple registered services of same
    * type and must be then specified when injecting the service. Please note that because the
    * [[Context]] is immutable, new updated instance will be returned after calling this method.
    *
    * @param value service to register
    * @param key   optional key
    * @param ct    class tag evidence
    * @tparam T type of the service
    * @return new updated instance of [[Context]]
    */
  def register[T <: AnyRef](value: T, key: String)(implicit ct: ClassTag[T]): Context = register[T](value, Some(key))


  /**
    * Registers several services to the ''Akkamo'' context, such services  then available to any other
    * module via one of the `inject` methods. This method allows to register service with ''key'',
    * which is primarily used to disambiguate between multiple registered services of same
    * type and must be then specified when injecting the service. Please note that because the
    * [[Context]] is immutable, new updated instance will be returned after calling this method.
    *
    * @param values
    * @tparam T
    * @return
    */
  def register[T <: AnyRef](values: List[Initializable.Parsed[T]])(implicit ct: ClassTag[T]): Context = {
    values.flatMap { case (default, aliases, value) =>
      // lineraize to pairs (Option(key), T)
      (aliases.map(key => (Some(key), value)) ++ (if (default) (Option.empty[String], value) :: Nil else Nil))
    }.foldLeft(this) { case (ctx, (keyOpt, value)) =>
      // register each one
      ctx.register(value, keyOpt)
    }
  }

  /**
    * Returns all registered instance of service of type `T`, as a map where key is instance of
    * registered service and value is the set containing all keys, under the particular service
    * instance was registered.
    *
    * @param ct class tag evidence
    * @tparam T type of the service
    * @return map of all registered instances of service of type `T`
    */
  def registered[T <: AnyRef](implicit ct: ClassTag[T]): Map[T, Set[String]]
}

/**
  * Error thrown during failing operation on the [[Context]] (e.g. attemp to register value into
  * service, that has not been registered before).
  *
  * @param message detail message
  * @param cause   optional error cause
  */
case class ContextError(message: String, cause: Throwable = null) extends AkkamoError(message, cause)
