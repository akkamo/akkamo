# Persistent config module
This module provides support for persisted configuration. The default implementation provided by
this module uses *MongoDB* as a persistent storage.

## Module configuration
This module does not require any configuration.

## How to use in your module
At first, you must decide whether you will use the default implementation of this module,
`MongoPersistentConfigModule`, or you will implement the trait `PersistentConfigModule` and provide
your own. Regardless of the actual used implementation itself, the trait `PersistentConfigModule` is
always used for injection, not the implementation. Which implementation will be actually used is
specified by choosing the proper module implementation in the build description of your module:

- **Select default module implementation**  
  In this example, the `MongoPersistentConfigModule` is used as an implementation of the
  `PersistentConfigModule` trait. This must be defined by adding the proper dependency to your
  `build.sbt` file:
  
  ```scala
  libraryDependencies += "eu.akkamo" %% "akkamo-mongo-persistent-config" % versionHere
  ```

- **Inject service into your module**
  This module registers into the *Akkamo context* the `PersistentConfig` service. Further details
  are described in the next chapter.  
  ```scala
  class MyModule extends Module with Initializable {
    override def initialize(ctx: Context) = Try {
      // injects optional value of PersistentConfig
      val persistentConfigOpt: Option[PersistentConfig] = ctx.inject[PersistentConfig]
    
      // or inject direct value of Persistent config (throws exception if value not found)
      val persistentConfig: PersistentConfig = ctx.get[PersistentConfig]
    }

    // don't forget to add PersistentConfig dependency to your module
    override def dependencies(dependencies: Dependency): Dependencies =
      dependencies.&&[PersistentConfig]
  }
  ```

## Provided APIs
This module registers into the *Akkamo context* following services:

- `PersistentConfig` - provides following methods for managing the properties:
  - `def get[T](alias: String)(implicit r: Reader[T]): Future[T]`  
    Returns the value of property specified by the `alias`.
  - `def store[T](alias: String, value: T)(implicit writer: Writer[T])`  
    Stores the property `value` under the specified `alias`.
  - `def remove(alias: String)`  
    Removes the property specified by the `alias`.

## Module dependencies
The default provided implementation of the `PersistentConfigModule`, the
`MongoPersistentConfigModule`, has dependencies on following core modules:

- [Config module](config-module.md)
- [Log module](log-module.md)
- [ReactiveMongo module](reactivemongo-module.md)