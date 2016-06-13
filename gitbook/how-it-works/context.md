# Akkamo Context

*Akkamo* context is in general the shared, mutable context, shared between all modules. It allows each module to inject services, provided by modules declared as module's *dependencies*, and/or register its own services for other modules.

## Injecting services
Injection of services, provided by modules declared as *dependencies*, can be performed during the *Init* or *Run* stages of the *Akkamo* lifecycle. Injection is done using one of the `inject` methods, available in the `Context` class. The mentioned two variants of the `inject` method are following:

* `inject[T](key: String, strict: Boolean = false)(implicit ct: ClassTag[T]): Option[T]` - This method returns instance of given service, specified by the type `T` and registered under the `key`. If no service is registered under the `key` and `strict` is set to `false`, the *default* instance of the service is returned.
* `inject[T](implicit ct: ClassTag[T]): Option[T]` - This method returns always the *default* instance of the service, specified by the type `T` (if available).

## Registering services
Module can register its own services for other modules using the `register` method on the `Context` class. The service registration should always be performed during the *Init* stage of the *Akkamo* lifecycle. Following methods can be used for registration:

* `register[T<: AnyRef](value: T, key: Option[String] = None)(implicit ct: ClassTag[T]): Unit` - Registers the instance of service `T` under the `key` (optional). Combination of the `T` type and `key` must be always unique.
