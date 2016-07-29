# Module dependencies
Very often your custom module will need to use services provided by another modules. This chapter
describes recommended practices when defining and working with dependencies.

## Always properly define dependencies
As described in chapter [Akkamo Module](../how-it-works/module.md), **always** pay extra attention
when defining your module dependencies. Dependency must be defined for each module, which service
is injected by your module. If not done so, you risk very nasty and unexpected runtime errors, as
due to the incorrect dependency definition the *Akkamo* may initialize module in incorrect order.

## Use own alias when injecting dependency
Let's start with simple example. Consider having module `SimpleStorageModule`, which depends on
[ReactiveMongo module](../modules/reactivemongo-module.md), as it needs access to the *MongoDB*. It
is clean that you need to inject provided `ReactiveMongoApi`, but using which *key*?

The answer is: always use your *key*. One might complain that under that key, there is probably no
such service registered in *Akkamo context*, but don't forget that when service for specific *key*
is not found, *default* instance of service should exist. Main goal of this recommendation is to
always keep the decision which service instance to use on module user, with fallback to *default*
service instance. Simple example is shown below:

```scala
class SimpleStorageModule extends Module with Initializable {

  val ContextKey: String = "SimpleStorage"

  override def initialize(ctx: Context) = Try {
    // Using this, adopter of your module can configure dedicated MongoDB connection with
    // alias 'SimpleStorage'. If no such service is provided, default service instance is used.
    val mongoApi: ReactiveMongoApi = ctx.get[ReactiveMongoApi](ContextKey)
    
    // ... rest of the code
  }

  // don't forget to add ReactiveMongo module dependency to your module
  override def dependencies(dependencies: Dependency): Dependencies =
    dependencies.&&[ReactiveMongoModule]
}
```