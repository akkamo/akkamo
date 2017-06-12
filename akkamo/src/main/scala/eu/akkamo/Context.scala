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
    * Future self type
    */
  type SelfType

  /**
    * Creates new instance of particular [[Registry]] implementation, with the new given value.
    *
    * @param p new value
    * @return new instance of particular [[Registry]] implementation
    */
  def copyWith(p: T): SelfType
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
    * Get service registered in ''Akkamo'' context, using the ''alias''.<br/>
    * Service alias must be defined in configuration.
    *
    * @param alias  mapping identifier
    * @param ct implicit ClassTag definition
    * @tparam T type of the service
    * @return implementation of interface `T` encapsulated in `Some` if exists else `None`
    */
  def getOpt[T](alias:String)(implicit ct: ClassTag[T]): Option[T]


  /**
    * Get service registered in ''Akkamo'' context, using the ''alias''.<br/>
    * Service alias is volatile in this case. If missing, the default value is used

    * @param alias
    * @param ct
    * @tparam T
    * @return implementation of interface `T` encapsulated in `Some` if exists else `None`
    */
  def getOpt[T](alias:Option[String] = None)(implicit ct: ClassTag[T]): Option[T]


  /**
    * Get default service registered in ''Akkamo'' context.

    * @param ct
    * @tparam T
    * @return implementation of interface `T` encapsulated in `Some` if exists else `None`
    */
  def getOpt[T](implicit ct: ClassTag[T]): Option[T] = getOpt()

  /**
    * Get service registered in ''Akkamo'' context, using the ''alias''.<br/>
    * Service alias must be defined in configuration.
    *
    * @param alias  mapping identifier
    * @param ct implicit ClassTag definition
    * @tparam T type of the service
    * @return implementation of interface `T`
    */
  @throws[ContextError]
  def get[T](alias:String)(implicit ct: ClassTag[T]): T =
    getOpt[T](alias).getOrElse(
      throw ContextError(s"The bean: ${ct.runtimeClass.getName} under alias: ${alias} doesn't exists"))


  /**
    * Get service registered in ''Akkamo'' context, using the ''alias''.<br/>
    * Service alias is volatile in this case. If missing, the default value is used.

    * @param alias
    * @param ct
    * @tparam T
    * @return implementation of interface `T` encapsulated in `Some` if exists else `None`
    */
  @throws[ContextError]
  def get[T](alias:Option[String] = None)(implicit ct: ClassTag[T]):T =
    getOpt[T](alias).getOrElse(
      throw ContextError(s"The bean: ${ct.runtimeClass.getName} under alias: ${alias.getOrElse("???")} doesn't exists"))


  /**
    * Get default service registered in ''Akkamo'' context.
    * @param ct
    * @tparam T
    * @return implementation of interface `T` encapsulated in `Some` if exists else `None`
    */
  @throws[ContextError]
  def get[T](implicit ct: ClassTag[T]):T = get()

  /**
    * Registers value into service, specified by its type and optional ''alias'', previously
    * registered to the ''Akkamo'' context. In fact, this serves as a shorthand for manually
    * injecting the desired service from the context, and then registering the selected value into
    * it.
    *
    * @param x   value to register
    * @param alias optional alias, under which the service was previously registered into the context, if None default is used
    * @param ct  class tag evidence
    * @tparam T type of previously registered service
    * @tparam X type of value to be registered into the service
    * @throws ContextError if operation cannot be completed for various reasons
    * @return updated instance of immutable [[Context]]
    */
  @throws[ContextError]
  def registerIn[T <: Registry[X], X](x: X, alias: Option[String] = None)(implicit ct: ClassTag[T]): Context


  /**
    * Registers value into service, specified by its type and optional ''alias'', previously
    * registered to the ''Akkamo'' context. In fact, this serves as a shorthand for manually
    * injecting the desired service from the context, and then registering the selected value into
    * it.
    *
    * @param x   value to register
    * @param alias mandatory alias, under which the service was previously registered into the context
    * @param ct  class tag evidence
    * @tparam T type of previously registered service
    * @tparam X type of value to be registered into the service
    * @throws ContextError if operation cannot be completed for various reasons
    * @return updated instance of immutable [[Context]]
    */
  @throws[ContextError]
  def registerIn[T <: Registry[X], X](x: X, alias: String)(implicit ct: ClassTag[T]): Context


  /**
    * Registers service to the ''Akkamo'' context, such service is then available to any other
    * module. This method allows to register service with optional
    * ''alias'', which is primarily used to disambiguate between multiple registered services of same
    * type and must be then specified when injecting the service. Please note that because the
    * [[Context]] is immutable, new updated instance will be returned after calling this method.
    *
    * @param value service to register
    * @param alias   optional alias, if None default alias is used
    * @param ct    class tag evidence
    * @tparam T type of the service
    * @return new updated instance of [[Context]]
    */
  def register[T <: AnyRef](value: T, alias: Option[String] = None)(implicit ct: ClassTag[T]): Context


  /**
    * Registers service to the ''Akkamo'' context, such service is then available to any other
    * module. This method allows to register service with ''alias'',
    * which is primarily used to disambiguate between multiple registered services of same
    * type and must be then specified when injecting the service. Please note that because the
    * [[Context]] is immutable, new updated instance will be returned after calling this method.
    *
    * @param value service to register
    * @param alias  mandatory alias
    * @param ct    class tag evidence
    * @tparam T type of the service
    * @return new updated instance of [[Context]]
    */
  def register[T <: AnyRef](value: T, alias: String)(implicit ct: ClassTag[T]): Context = register[T](value, Some(alias))


  /**
    * Registers several services to the ''Akkamo'' context, such services  then available to any other
    * module via one of the `inject` methods. This method allows to register service with ''alias'',
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
      // lineraize to pairs (Option(alias), T)
      (aliases.map(key => (Some(key), value)) ++ (if (default) (Option.empty[String], value) :: Nil else Nil))
    }.foldLeft(this) { case (ctx, (keyOpt, value)) =>
      // register each one
      ctx.register(value, keyOpt)
    }
  }

  /**
    * Returns all registered instance of service of type `T`, as a map where alias is instance of
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
