# Akkamo Module

*Akkamo module* is the cornerstone of each modular application based on this platform. Module is
nothing more than an old-good Scala class, implementing appropriate *traits* and their methods.
Each module is represented by the class implementing at least `eu.akkamo.Module` trait. For module
lookup during *Akkamo* startup, Java's
[ServiceLoader](https://docs.oracle.com/javase/8/docs/api/java/util/ServiceLoader.html) mechanism
is used, therefore for each *Akkamo* module a new line with *fully qualified domain name* of
module's class must be added to the `eu.akkamo.Module` file located in the `META-INF/services/`
directory of module's JAR file.

## Creating new Akkamo module
Akkamo module is nothing special but plain Scala class, implementing several traits. Each module
must implement the `eu.akkamo.Module` trait, example of the simplest possible module is below:

```scala
class MyModule extends Module {

  override def dependencies(dependencies: Dependency): Dependency = dependencies
}
```

Method `dependencies` needs some deeper explanation. If our module interacts with any other module
(e.g. injects its registered services), such module **must** be declared as our module's dependency.
This is done exactly using the `dependencies` method, using the following syntax:

```scala
override def dependencies(dependencies: Dependency): Dependency =
  dependencies.&&[ModuleA].&&[ModuleB]
```

The above example defines the `ModuleA` and `ModuleB` modules as dependencies.

> **Warning** Please pay attention to always properly define your module dependencies. If not done
so, dependend modules may not be properly initialized during the Akkamo initialization, which may
lead to unexpected runtime errors.

## Module lifecycle
*Akkamo module* allows to run specific application code during every
[Akkamo lifecycle](lifecycle.md). For every lifecycle stage, there is appropriate Scala trait, which
has to be extended and implemented in order to run code during the selected lifecycle stage. Below
is the overview of available lifecycle Scala traits.

### Initialization stage
In order to run application code during the *initialization stage*, the trait
*eu.akkamo.Initializable* must be extended, as in following example:

```scala
class MyModule extends Module with Initializable {
  override def initialize(ctx: Context): Res[Context] = Try {
    // application code here
  }
}
```

The `initialize` method is the called during the initialization stage of the *Akkamo lifecycle*.
This method may return [Try](http://www.scala-lang.org/api/current/index.html#scala.util.Try) or
[Future](http://www.scala-lang.org/api/current/index.html#scala.concurrent.Future) which is
implicitly converted into the `Res[Context]`.

> **Info** In this lifecycle stage, you should register all your module services. Never do this in
any later stage.

### Run stage
In order to run application code during the *run stage*, the trait
*eu.akkamo.Runnable* must be extended, as in following example:

```scala
class MyModule extends Module with Runnable {
  override def run(ctx: Context): Res[Context] = Try {
    // application code here
  }
}
```

### Dispose stage
In order to run application code during the *dispose stage*, the trait
*eu.akkamo.Disposable* must be extended, as in following example:

```scala
class MyModule extends Module with Disposable {
  override def dispose(ctx: Context): Res[Unit] = Try {
    // application code here
  }
}
```

## Registering module into Akkamo
When your module is ready, it must be explicitly registered into the *Akkamo*, in order to find and
run it during the application startup. As described in the beginning of the chapter, *Akkamo* uses
the [ServiceLoader](https://docs.oracle.com/javase/8/docs/api/java/util/ServiceLoader.html)
mechanism. To register the module, following steps must be done:

- in the module *JAR* file, create directory `META-INF/services`
- in that directory, create new file called `eu.akkamo.Module`
- each line of this file should contain *fully qualified domain name* of your module
  (e.g. `my.company.FooModule`), one line per module