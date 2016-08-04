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
```
{
  routeRegistryAlias="akkamo.webContent"
  default = true
  routeGenerators = [
    {
      prefix = "web"
      class = "eu.akkamo.web.FileFromDirGenerator"
    }
  ]
}
```

### Configuration keys


- `aliases` *(optional)* - defines the array of *alias names*, under which the web content registry will be registered
    to the *Akkamo* context

- `default` *(optional)* - `true/false`
    If number of web content registries is more then one at least one of them has to be defined as default.
    If only single content registry is configured *default* option is not required.

- `routeRegistryAlias` *(optional)* - web content registry alias in akka http

- `routeGenerators` *(optional)* - Array of content generators configurations

- `prefix` *(optional)* - relative path that content generator is listening on

- `class` - Full name of class serving as content generator (e.g. class that loads file from file system)

- `parameters` *(optional)* - Additional parameters passed to content generator constructor (e.g. directory for static content)


### Configuration example

```
akkamo.webContent {
  // empty WebContentRegistryCreated
  // RouteGenerators must be added manualy
  name1 {
    routeRegistryAlias = "akkaHttpAlias"
    aliases = ["alias1", "alias2"]
  }
  // WebContentRegistryCreated containing one RouteGenerator serving content
  // of webStaticContent directory and listening on .../web
  name2 {
    default = true // at least in one configuration is value mandatory if the number of instances is > 1
    routeGenerators = [
      {
       prefix = "web"
       class = "eu.akkamo.web.FileFromDirGenerator"
       parameters = ["/webStaticContent"]
      }
    ]
  }
}
```


## How to use in your module
Each configured web content registry is registered into the *Akkamo context* and selected
`ContentMapping` can be directly injected into it using the `registerIn` method of the
*Akkamo content*. Please note that the *Akkamo context* object is immutable, hence the `registerIn`
method returns new instance of the context object, rather than modifying the current instance.

```scala
class MyModule extends Module with Runnable {

  override def run(ctx: Context): Res[Context] = Try {
    val cm: ContentMapping = ???  // your ContentMapping
    
    // Possible ways how to register the content mapping into web content registry:
    
    // registers the content mapping into WebContentRegistry, configured with name 'name1'
    ctx.registerIn[WebContentRegistry, ContentMapping](cm, "name1")
    
    // registers the content mapping into WebContentRegistry, marked as default ('name2')
    ctx.registerIn[WebContentRegistry, ContentMapping](cm)
    
    // registers the content mapping into WebContentRegitry, configured for alias 'alias2'
    ctx.registerIn[WebContentRegistry, ContentMapping](cm, "alias2")
    
    // ... rest of the method code
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

- [Akkamo Akka Http](akka-http-module.md)
