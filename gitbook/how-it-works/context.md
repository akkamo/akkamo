# Akkamo Context

*Akkamo context* represents the central point of the *Akkamo*  platform, where different modules can
register own services provided to other modules, or from where already registered services can be
injected.

## Registering service into context
Every module can register own services into the *Akkamo context* using one of the following methods:

* `def register[T <: AnyRef](value: T, key: Option[String] = None)(implicit ct: ClassTag[T]): Context`  
This method registers the service instance into the *Akkamo context*, such service is then available
to any other module via one of the `inject` methods. This method allows to register service under
the specified *key*,, which is primarily used to disambiguate between multiple registered services
of the same type and must be then used when injecting the service. Please note that because the 
*Akkamo context* is immutable, new updated instance of context will be
returned.

* `def register[T <: AnyRef](value: T, key: String)(implicit ct: ClassTag[T]): Context`  
This method provides same functionality as previous, but the *key* parameter is not optional in this
one.

> **Info** Please note that registering services into the context should be allways
done during the *Init* stage of the *Akkamo* lifecycle.

> **Info** The `register` method always return a new instance Of `Context`.

## Registering bean into registered service
Instead of use mutable services we recommends to use immutable service bean that implements trait
```eu.akkamo.Registry[Route]```.Typical example can be found in the  HttpModule. HttpModule registers
several (at least one) `RouteRegistry` instances. Each RouteRegistry instance implements ```Registry[Route]``` trait.
Thus the `RouteRegistry` is able to create a self copy (via method ```copyWith(p: Route): this.type```) containing
the new instance of Route.<br/> There is definition of ```Route``` trait:

```scala
trait Registry[T] {
 def copyWith(p: T): this.type
}
```
On the context exist method:
* ```def registerIn[T <: Registry[X], X](x: X, key: Option[String] = None)(implicit ct: ClassTag[T]): Context```
that realise update of instance `T` with instance of X for given `key`.

Typical usage of register method looks like:
```scala
ctx.registerIn[RouteRegistry, Route](route, Some("key"))
```
> **Hint** Also method `register` with the parameter `key` as plain `String` is defined.

> **Info** The `register` method always return a new instance Of `Context`.

## Injection services from context
Injection of services, previously registered into the *Akkamo context*, can be done using one of the
`inject` or `get` methods, during the *Init* or *Run* stages of the *Akkamo* lifecycle. Following
methods are available for injecting already registered services:

* `def inject[T](implicit ct: ClassTag[T]): Option[T]`  
Injects service which has been previously registered into the *Akkamo context* as a *default*
service, i.e. without any identifier `key`.

* `def get[T](implicit ct: ClassTag[T]): T`  
Provides same functionality as method above, but instead of optional result returns service
instance, or throws `ContextError` if no instance was found.

* `def inject[T](key: String, strict: Boolean = false)(implicit ct: ClassTag[T]): Option[T]`  
Injects service which has been previously registered into the *Akkamo context*. If an `key` was used
during registration, it must be specified now. If no service instance exists for given key,
*default* service is returned (i.e. service which has been registered without any identifier `key`).
This behaviour can be disabled by setting the `strict` parameter to `true`.

* `def get[T](key: String, strict: Boolean = false)(implicit ct: ClassTag[T]): T`  
Provides same functionality as method above, but instead of optional result returns service
instance, or throws `ContextError` if no instance was found.
