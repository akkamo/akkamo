# Web content module
This module provides support for serving static data from particural resource (e.g. static files, directories or
some generated content from resource). Combination of route and content generator we call *route generator*.

## Module configuration

This module requires its configuration under the `akkamo.webContent` namespace.
Each configuration block under this namespace represents the configuration of single instance of web content registry registered
into the Akkamo context.

There are two possible configuration approaches. First one is to set empty web content
registry and add content generators manual via API (see example *name1*).
Or second one is to configure all settings directly into configuration file (see example *name2*).


If no configurations is provided, web content module is self configured with following options:
```scala
{
  routeRegistryAlias="akkamo.webContent"
  default = true
  routeGenerators = [{
   prefix = Web
   class = eu.akkamo.web.FileFromDirGenerator
   }]
}
```

### Configuration keys


- `aliases` *(optional)* - defines the array of *alias names*, under which the web content registry will be registered
    to the *Akkamo* context

- `default` *(optional)* - `true/false`
    If number of web content registries is more then one at least one of them has to be defined as default.
    If only single content registry is configured *default* option is not required.

- `routeRegistryAlias` *(optional)* - web content registry alias in akka http

- `generators` *(optional)* - Array of content generators configurations

- `prefix` *(optional)* - relative path that content generator is listening on

- `class` - Full name of class serving as content generator (e.g. class that loads file from file system)

- `parameters` *(optional)* - Additional parameters passed to content generator constructor (e.g. directory for static content)


### Configuration example

```scala
akkamo.webContent = {
  // empty WebContentRegistryCreated
  // RouteGenerators must be added manualy
  name1 = {
    routeRegistryAlias = "akkaHttpAlias"
    aliases = ["alias1", "alias2"]
  },
  // WebContentRegistryCreated containing one RouteGenerator serving content
  // of webStaticContent directory and listening on .../web
  name2 = {
    default = true // at least in one configuration is value mandatory if the number of instances is > 1
    generators = [
      {
       prefix="web"
       class = eu.akkamo.web.FileFromDirGenerator
       parameters = ["/webStaticContent"]
      }
    ]
  }
}
```


## How to use in your module

Each configured web content registry  is registered into the *Akkamo* context and available for injection,
using the following rules:

* **inject by the configuration name**
  Selected web content registry can be injected using its configuration name, e.g.
  `ctx.inject[WebContentRegistry]("wcr1")`
* **inject the default  web content registry**
  If any configured web content registry has set the `default = true` property, it will be considered as
  *default* web content registry and can be injected without specifying the key, e.g.
  `ctx.inject[WebContentRegistry]`. Please note that only one configured web content registry can be specified
  as *default*.
* **inject by the name alias**
  Besides the configuration name, each configured web content registry can be identified by one or more
  *alias name*. Such *alias name* can be used to inject the selected web content registry, e.g.
  `ctx.inject[WebContentRegistry]("alias3")`.

  Below is example code of very simple module, injecting actor systems configured by configuration
  example shown above.

```scala
class MyModule extends Module with Initializable {
  override def initialize(ctx: Context) = Try {
    // injects the web content registry marked as default ('name2' in this case)
    val wcr1: Option[WebContentRegistry] = ctx.inject[WebContentRegistry]

    // same as above, with explicitly specified name
    val wcr1_1: Option[WebContentRegistry] = ctx.inject[WebContentRegistry]("name2")

    // injects the connection 'name1', which has defined the alias 'alias1'
    val wcr2: Option[WebContentRegistry] = ctx.inject[WebContentRegistry]("alias1")

    // ... rest of method code ...

  }

  // don't forget to add WebContentModule dependency to your module
  override def dependencies(dependencies: Dependency): Dependencies =
    dependencies.&&[WebContentModule]
}
```


### Provided APIs
This module registers into the *Akkamo context* following services:

- `WebContentRegistry` - provides following methods for managing the properties:

  - `def mapping: Map[String, RouteGenerator]`
  Getter for mapping *prefix* to content generator

  - `def aliases: List[String]`
  Getter for web content registry aliases in akkamo context

  - `def routeRegistryAlias: Option[String]`
  Getter for web content registry alias in akka http

  - `def default: Boolean`
  Getter for default flag of web content registry

## Provided content generators

### FileFromDirGenerator
`class FileFromDirGenerator(source: File)` - This content generator provides static content
from files or directories in given location `source: File`.


## Module dependencies
This module depends on following core modules:

* [Akkamo Akka Http](akka-http-module.md)
