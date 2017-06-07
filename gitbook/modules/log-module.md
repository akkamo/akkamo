# Log module
TODO review documentation - new multiple implementations 

This module provides logging support for non-*Akka* components, using the standard
[Akka Logging](http://doc.akka.io/docs/akka/current/scala/logging.html) mechanism.

## Module configuration
This module doesn't require any special configuration, except properly configured *Akka* actor
system. By default, this module tries to use actor system with name specified in constant
`LogModule#LoggingActorSystem`. If no such actor system is found, *default* actor system is tried
(e.g. actor system configured with `default = true` option). If none suitable actor system is found,
module initialization will fail.

## How to use in your module
This module registers `LoggingAdapterFactory` instance into the *Akkamo context*, using which the
instance of logger can be obtained (for more details about `LoggingAdapterFactory` see the next
chapter).

```scala
class MyModule extends Module with Initializable {
  override def initializable(ctx: Context) = Try {
    // injects the LoggingAdapterFactory
    val logFactory: LoggingAdapterFactory = ctx.get[LoggingAdapterFactory]
    
    // obtains the logger instance for this class
    val log: LoggingAdapter = logFactory(getClass)
    
    // ... rest of the method code
  }
  
  // don't forget to add Log module dependency to your module
  override def dependencies(dependencies: TypeInfoChain): TypeInfoChain =
    dependencies.&&[LoggingAdapter]
}
```

## Provided APIs
This module registers into the *Akkamo context* following services:

- `LoggingAdapterFactory`  
  Factory class used to obtain instance of
  [LoggingAdapter](http://doc.akka.io/api/akka/current/index.html#akka.event.LoggingAdapter) using
  the following method:s
  - `def apply[T](category: Class[T]): LoggingAdapter`  
    Returns instance of logging adapter for category specified by the class name
  - `def apply(category: AnyRef): LoggingAdapter`  
    Returns instance of logging adapter for category specified by any object

## Module dependencies
This module depends on following core modules:

* [Akka module](akka-module.md)