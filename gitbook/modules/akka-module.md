# Akka module

This module provides simple way to configure and run one or more [Akka](https://akka.io) actor
systems.

## Module configuration

This module requires its configuration to be available under the `akkamo.akka` namespace. The module
configuration is as simple as possible and in fact it just wraps one or *Akka* configurations for
one or more actor systems, with additional information about how such actor system will be
registered into the *Akkamo* context.

Each configuration block under the `akkamo.akka` represents the configuration of single *Akka* actor
system. For each configuration, instance of
[ActorSystem](http://doc.akka.io/api/akka/current/#akka.actor.ActorSystem) will be registered into
the *Akkamo* context  and available for injection.

### Configuration keys

* `aliases` *(optional)* - defines the array of *alias names*, under which the actor system will be
  registered to the *Akkamo* context
* `default` *(optional)* - `true/false` whether the actor system will be available for injection as
  *default* (Please note that only one configured actor system can be specified as *default*)
* `akka` - configuration subtree of the *Akka* itself, see
   [official documentation](http://doc.akka.io/docs/akka/current/general/configuration.html#Custom_application_conf)
   for more details

Example of module configuration:

```
akkamo.akka {
  // one block with akka configuration contains several aliases with the name name
  someActorSystem1 {
    aliases = ["as1", "system1"]
    default = true
    // standard akka attributes for example:
    akka {
      // Akka configuration here
    }
  }
  someActorSystem2 {
    aliases = ["as2", "system2"]
    // standard akka attributes for example:
    akka{
      //  Akka configuration here
    }
  }
}
```

## How to use in your module
Each configured actor system is registered into the *Akkamo* context and available for injection,
using the following rules:

* **inject by the configuration name**  
  Selected actor system can be injected using its configuration name, e.g.
  `ctx.inject[ActorSystem]("someActorSystem1")`
* **inject the default actor system**  
  If any configured actor system has set the `default = true` property, it will be considered as
  *default* actor system and can be injected without specifying the alias, e.g.
  `ctx.inject[ActorSystem]`. Please note that only one configured actor system can be specified as
  *default*.
* **inject by the name alias**  
  Besides the configuration name, each configured actor system can be identified by one or more
  *alias name*. Such *alias name* can be used to inject the selected actor system, e.g.
  `ctx.inject[ActorSystem]("as1")`.

Below is example code of very simple module, injecting actor systems configured by configuration
example shown above.

```scala
class MyModule extends Module with Initializable {
  override def initialize(ctx: Context) = Try {
    // injects the actor system marked as default ('someActorSystem1' in this case)
    val as1: Option[ActorSystem] = ctx.inject[ActorSystem]

    // same as above, with explicitly specified name
    val as11: Option[ActorSystem] = ctx.inject[ActorSystem]("someActorSystem1")

    // injects the actor system 'someActorSystem2', which has defined the alias 'as2'
    val as2: Option[ActorSystem] = ctx.inject[ActorSystem]("as2")

    // ... rest of method code ...

  }

  // don't forget to add ActorSystem dependency to your module
  override def dependencies(dependencies: Dependency): Dependencies =
    dependencies.&&[ActorSystem]
}
```

## Provided APIs
This module registers into the *Akkamo* context following types:

* [ActorSystem](http://doc.akka.io/api/akka/current/#akka.actor.ActorSystem) for each configured
actor system, available for injection under the above specified rules

## Module dependencies
This module depends on following core modules:

* [Config module](config-module.md)
* [Log module](log-module.md)
