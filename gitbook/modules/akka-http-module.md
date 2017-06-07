# Akka HTTP module
This module adds support for the [Akka HTTP](http://doc.akka.io/docs/akka/current/scala/http/), a
client/server HTTP library built upon the [Akka](http://akka.io) framework.

## Module configuration
This module requires its configuration under the `akkamo.akkaHttp` namespace. Each configuration
block under this namespace represents the configuration of single HTTP connection and will register
into the *Akkamo context* instance of the `RouteRegistry` allowing to register HTTP routes for
specified HTTP connection.

### Configuration keys
- `aliases` *(optional)* - defines the array of *alias names*, under which the HTTP connection
  will be registered to the *Akkamo* context
- `default` *(optional)* - `true/false` whether the HTTP connection will be available for
  injection as *default* (Please note that only one configured connection can be specified as
  *default*)
- `port` - specifies the connection port
- `protocol` - specifies the protocol (e.g. `http`, `https`)
- `host` - connection host name (e.g. `localhost`)
- `akkaAlias` - name/alias of already configured *Akka* actor system, provided by
  [Akka module](akka-module.md), if not defined, *default* registered actor system will be used
- `requestLogLevel` *(optional)* - defined log level for request level logging, default `off` means
  no logging
- `useMDC` - *(optional)* - `true/false` (default false) defines usage of logger than support custom 
[MDC](http://logback.qos.ch/manual/mdc.html), in this case headers are mapped in to MDC     
- `requestLogFormat` *(optional)* - defined log format, defaults to
   "%1s %2s: HTTP/%3s headers:%4s" or "%1s %2s: HTTP/%3s" (if useMDC=true) if no custom format defined

Example of fully working configuration is shown below:

```
akkamo.akkaHttp = {
  // complete configuration with several name aliases
  name1 = {
    aliases = ["alias1", "alias2"]
    port = 9000 // port, not mandatory
    protocol = "http" // http, https, ...
    host = "localhost" // host, default localhost
    akkaAlias = "alias" // not required, default is used if exists
    requestLogLevel = "info"  // defines level for request level logging. Default "off" means no logging
  }
}
```

The above configuration creates one HTTP connection, registered into the context under its name
`name1` and aliases `alias1` and `alias2`.

### Request log formatting
If the request logging is enabled (using the `requestLogLever` option set to other than `off`),
custom log format can be specified using own log string with parameters placeholders, that are
automatically replaced by actual values. Example of such log string is shown below:

```
"%1s %2s: HTTP/%3s headers:%4s"
```

Each parameter is enclosed between the `%{` and `}` strings, e.g. `%{PARAM_NAME}`. Recommended param
name style is uppercase with words separated using underscore.

At this moment, following parameters are available for use:

1. is replaced with the actual request HTTP method (e.g. `POST`)
2. is replaced with the actual request reklative uri URI
3. is replaced with the actual response status (e.g. `403 Forbidden`)
4. is replaced with list of headers (only if useMDC=false)


## How to use in your module
Each configured connection registers into the *Akkamo context* instance of `RouteRegistry`,
inside which single *routes* for such connection are registered. For more info about
*Akka HTTP* routes, see
[the offical documentation](http://doc.akka.io/docs/akka/current/scala/http/routing-dsl/index.html)

Below is the example code, showing how to register own routes into `RouteRegistry` configured using
the sample configuration above.

```scala
class MyModule extends Module with Initializable {
  override def run(ctx: Context) = Try {
  val route: Route = {
    get {
      path("test") {
        complete("hello, world!")
      }
    }
  }
  
  // registers the route into already registered RouteRegistry for configuration named 'name1'
  ctx.registerIn[RouteRegistry, Route](route, Some("name1"))
  
  // OR the same as above, but using the alias
  ctx.registerIn[RouteRegistry, Route](route, Some("alias2"))

    // ... rest of method code ...

  }

  // don't forget to add RouteRegistry dependency to your module
  override def dependencies(dependencies: TypeInfoChain): TypeInfoChain =
    dependencies.&&[RouteRegistry]
}
```

## Provided APIs
This module registers into the *Akkamo* context following services:

- `RouteRegistry` - registry, allowing to register HTTP routes to selected connection

## Module dependencies
This module depends on following core modules:

- [Akka module](akka-module.md)
- [Config module](config-module.md)
