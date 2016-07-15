# Akkamo Lifecycle

Each *Akkamo* applications is nothing more than a set of selected modules. The*Akkamo* platform itself is the core,
which brings those modules into the life and joins them into fully working application. Before explaining the module
architecture itself, the lifecycle of the *Akkamo* application must be described in more details.

When *Akkamo* application starts, first step is to find all available modules at the classpath. For this purpose,
*Akkamo* uses the standard mechanism of Java,
called [Service loader](https://docs.oracle.com/javase/8/docs/api/java/util/ServiceLoader.html).
Some modules, however, may declare dependencies on other modules, i.e. dependent modules must be always initialized
before, so the correct order is calculated. Once correctly ordered, the first lifecycle stage, *initialization*,
is then performed.

## Init stage
This is the very first stage of *Akkamo* lifecycle, called right after the correct order of modules is achieved.
This is the stage where module should register its own functionality into the *context* (if necessary). In order
that a module can perform some logic in this stage, the `Initializable` interface and its method
`initialize(ctx: Context): Res[Context]` must be implemented.<br/>
Actual version of *Akkamo* supports next implicit conversion to `Res` automatically:

## Run stage
After the initialization stage, when all modules are successfully initialized, this stage is executed. In order
to execute custom code in a module in this stage, the interface `Runnable` and its method `run(ctx: Context): Res[Context]`
must be implemented.

## Dispose stage
The first two stages, *Init* and *Run*, are performed during the application startup. This stage is performed when
the JVM gets the OS signal to stop, allowing modules to gracefully release allocated resources, close opened files,
ports, etc. In order that a module can perform some logic in this stage, the `Disposable` interface and its method
`dispose(ctx: Context): Res[Unit]` must be implemented.

#### Implicit conversions
Actual implementation of *Akkamo* supports several implicit conversion to ```Res[Context]``` or ```Res[Unit]```, namely from:
* `Context` :
  ```Scala
    override def initialize(ctx: Context) = {
      ...
      ctx.register(...)
    }

  ```

* `Try[Context]` :
  ```Scala
    override def initialize(ctx: Context) = Try {
    ...
      ctx.register(...)
    }

  ```
* `Future[Context]` :
  ```Scala
    override def initialize(ctx: Context) = Future {
    ...
      ctx.register(...)
    }

  ```

#### Ordering

The modules form a dependency tree.<br/>
Next simple rules works during module lifecycle management:

* Instances of modules are created in let say `random` order.
* if module __A__ depends on module __B__ then `initialize` method of __B__ is called before call of `initialize` on __A__.
* if module __A__ depends on module __B__ then `run` method of __A__ is called before call of `run` method on __B__.
* if module __A__ depends on module __B__ then `dispose` method of __A__ is called before call of `dispose` method on __B__.

The rules get the possibility to have only two stages when system is initialized.<br/>
For example, if an module depends on __AkkaHttp__ module then also routes appending
works in `run` method, because "run" of Http module follows later.