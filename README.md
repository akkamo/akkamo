# Makka - modules in Akka.
Runtime assembly of several modules running on top of Akka.
> This project is experimental, please do not use for production purposes as it may change dramatically. Any feedbacks, suggestions or participations are welcomed!

## 1. About Makka.
Makka system allows the construction of a set of modules that can cooperate together, or run independently.
Application is assembled via [sbt-native-packager sbt plugin](https://github.com/sbt/sbt-native-packager)
as native application (zip file with starting scripts). Consult [demo application](https://github.com/JurajBurian/makka-demo) for details.<br/>
For configuration of Makka Application is [Lighbend configuration library](https://github.com/typesafehub/config) used

## 2. How it works
Each module is represented by the class implementing the `com.github.jurajburian.makka.Module` trait. For module lookup during _Makka_ startup, Java's [ServiceLoader](https://docs.oracle.com/javase/8/docs/api/java/util/ServiceLoader.html) mechanism is used, therefore for each _Makka_ module a new line with _fully qualified domain name_ of module's class must be added to the `com.github.jurajburian.makka.Module` file located in the `META-INF/services/` directory of module's JAR file.

### 2.1 Lifecycle

When `Makka` is started, classpath is searched for all modules, registered using the _ServiceLoader_ mechanism. Then several lifecycle stages are performed:

1. __Init stage__ - Very first stage of _Makka_ module lifecycle, module should validate whether all its possible dependencies are already initialized and/or register own APIs/services into the _Makka_ context. To execute module code in this stage, trait `com.github.jurajburian.makka.Initializable` must be mixed in module's class and method `initialize(ctx: Context): Boolean` implemented. In this method the _Makka_ context is provided, allowing to lookup dependency or register own service. If not all module's dependencies are ready yet, `false` should be returned and module initialization will be repeated again later, `true` is indicating that module is successfully initialized.
2. __Run stage__ - When all modules are initialized, this stage is performed. To execute module code in this stage, trait `com.github.jurajburian.makka.Runnable` must be mixed in module's class and method `run(ctx: Context): Unit` implemented.
3. __Dispose stage__ - This stage is performed just before the _Makka_ system is shutted down, usually when the JVM gets signal to terminate. It allows module to gracefully close all allocated resources, opened ports, files, etc. To execute module code in this stage, trait `com.github.jurajburian.makka.Disposable` must be mixed in module's class and method `dispose(ctx: Context): Unit` implemented.

> Please note that the Makka initialization process (including calling of `initialize` and `run` methods) is executed in single thread, thus module required to call asynchronous code should allways wait until such code is finished.

### 2.2 Context
`Context` is stateful entity managing published "services" and the state of initialization process:
```Scala
def inject[T](implicit ct:ClassTag[T]):Option[T]
def inject[T](key:String, strict:Boolean = false)(implicit ct:ClassTag[T]):Option[T]
def register[T<:AnyRef](value:T, key:Option[String] = None)(implicit ct:ClassTag[T])
def initialized[T<:(Module with Initializable)](implicit ct:ClassTag[T]):Boolean
def initializedWith[T<:(Module with Initializable)](implicit ct:ClassTag[T]):With
def running[T<:(Module with Runnable)](implicit ct:ClassTag[T]):Boolean
def runningWith[T<:(Module with Initializable)](implicit ct:ClassTag[T]):With
```
Methods:
* register - register service T under optional key. Combination of type T and key must be unique.
Simple example is `ConfigModule` that provides instance of `com.typesafe.config.Config` registered without any key.

	See:
	```Scala
	override def initialize(ctx: Context): Boolean = {
	  ctx.register(ConfigFactory.defaultApplication())
	  true
	}
	```
* inject, there are two methods
	1. without parameter - this method returns "default" instance of the service if exists
	2. with parameter `key` and `strict` - this method returns instance of service registered under given `key`,
	if nothing is registered under key and `strict` is equal `true` then default value is returned if exists.
	> todo why so benevolent system + plugin

### 2.3 Order
>todo - dependencies are managed by order

## 3. Existing modules

### 3.1 Built-in modules
1. _ConfigModule_ - provides application-wide configuration mechanism, based on [Lightbend Config](https://github.com/typesafehub/config)
2. _AkkaModule_ - provides [Akka](http://akka.io) actor system

### 3.2 Third-party modules
1. [makka-reactivemondo](https://github.com/JurajBurian/makka-reactivemondo) - provides [Mongo DB](https://www.mongodb.com) database support, using the [http://reactivemongo.org](http://reactivemongo.org) driver
2. [makka-persitent-config](https://github.com/JurajBurian/makka-persistent-config) - provides persistent configuration functionality (default implementation uses [Mongo DB](https://www.mongodb.com) as a persistent storage)

## 4. How to write Module, conventions & rules
1. every module that publish a service should provide zero configuration capability if it is possible.
> todo - conventions, key aliases .....

