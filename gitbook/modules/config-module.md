# Config module
This module provides configuration support, using the standard
[Typesafe Config](https://github.com/typesafehub/config) library.

# Module configuration
Module does not have a configuration.

## How to use in your module
This module registers `cpm.typesafe.Config` instance into the *Akkamo context*.

```scala
class MyModule extends Module with Initializable {
  override def initializable(ctx: Context) = Try {
    // injects the configuration
    val config: Config = ctx.get[Config]
  }
  
  // don't forget to add Config module dependency to your module
  override def dependencies(dependencies: TypeInfoChain): TypeInfoChain =
    dependencies.&&[ConfigModule]
}
```

## Provided APIs
For more information visit Config definition in [Typesafe Config](https://github.com/typesafehub/config) library.

## Module dependencies
Module does not have any dependencies.