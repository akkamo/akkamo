# Makka - modules in Akka.
Runtime assembly of several modules running on top of Akka.
>Project is experimental don't use it please. Any suggestions are welcome !!!

## About Makka.
Makka system allows the construction of a set of modules that can cooperate together, or run independently.
Application is assembled via [sbt-native-packager sbt plugin](https://github.com/sbt/sbt-native-packager)
as native application (zip file with starting scripts). Consult [demo application](https://github.com/JurajBurian/makka-demo) for details.<br/>
For configuration of Makka Application is [Lighbend configuration library](https://github.com/typesafehub/config) used

## How it works
Each module is represented by the class implementing the `com.github.jurajburian.makka.Module` trait. For module lookup during _Makka_ startup, Java's [ServiceLoader](https://docs.oracle.com/javase/8/docs/api/java/util/ServiceLoader.html) mechanism is used, therefore for each _Makka_ module a new line with _fully qualified domain name_ of module's class must be added to the `com.github.jurajburian.makka.Module` file located in the `META-INF/services/` directory of module's JAR file.

If module want use a dependant module respectively want use API published by the module, then must implement
at least one of next interfaces:
```Scala
package com.github.jurajburian.makka
trait Initializable {
  def initialize(ctx:Context):Boolean
}

trait Runnable {
  def run(ctx:Context):Unit
}
```
If a module allocate resources like TCP/IP ports, can't be deallocated from the memory by GC,
 or need graceful shut down then need to implement next interface:
```Scala
package com.github.jurajburian.makka
trait Disposable {
  def dispose(ctx:Context):Unit
}
```
### Context
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

### Lifecycle
1. All modules created in random order
2. On each module - implementing `Initalizable` - is method `initialize` called with global `Context` as argument.
	* if method return `false` then module state is understand as not initialized and the execution will be repeated in future.
	* if method return `true` then module state is understand as initialized
3. On each module - implementing `Runnable` - is method `run` called with global `Context` as argument.
4. Application is initialized and running ....
5. when JVM gets signal causing end of JVM run then on each module - implementing `Disposable` -
is method `dispose` called with global `Context` as argument.

> Whole initialization process (including calls of run method) is executed in single thread.
 So if a module calls asynchronous processing then the whole processing thread should wait for the end of such calls.

### Order
>todo - dependencies are managed by order

## Build in modules
1. ConfigModule - configuration provider
2. AkkaModule - Akka ActorSystem provider
## Known modules
1. [makka-reactivemondo](https://github.com/JurajBurian/makka-reactivemondo) - reactive mongo provider
2. [makka-persitent-config](https://github.com/JurajBurian/makka-persistent-config) - persistent config provider, with default on top of mongo implementation

## How to write Module, conventions & rules
>todo - conventions, key aliases .....
   


