# Configmodule
This module provides configuration support, using the standard
[Typesafe Config](https://github.com/typesafehub/config) library.

#Module configuration
Module don't have a configuration.

## How to use in your module
This module registers `cpm.typesafe.Config` instance into the *Akkamo context*.

```scala
class MyModule extends Module with Initializable {
  override def initializable(ctx: Context) = Try {
    // injects the LoggingAdapterFactory
    val config= ctx.get[Config]
  }
  
  // don't forget to add Log module dependency to your module
  override def dependencies(dependencies: Dependency): Dependencies =
    dependencies.&&[ConfigModule]
}
```

## Provided APIs
For more information visit Config definition in [Typesafe Config](https://github.com/typesafehub/config) library.

## Module dependencies
Module don't have dependencies.

* [Akka module](akka-module.md)