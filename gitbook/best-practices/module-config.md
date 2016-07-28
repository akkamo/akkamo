# Module configuration
Careful design of the configuration structure is the important part of module design. Consistent,
clean and well-structured configuration, with comprehensive documentation will help other users to
easily adopt your module.

## Basic recommendations
*Akkamo platform* already provides mechanisms for loading configuration for your custom module, via
the following built-in modules:

- [Config module](../modules/config-module.md) - provides support for non-persistable, readonly
  configuration, using the [Typesafe Config](https://github.com/typesafehub/config) library
- [Persistent config module](../modules/persistent-config-module.md) - provides support for
  persistable configuration, with default implementation using the
  [MongoDB](https://www.mongodb.com) database as persistent storage

Regardless of the selected configuration mechanism, you should keep in ming the following
recommendations when designing your configuration structure:

### Use namespaces
Anyhow your project name might be unique, you should never use names without proper namespace as a
root for your module configuration. Consider the following examples:
```
## WRONG. This name may clash with other module's config and may cause ugly runtime problems.
httpServer {
  // config keys here
}
  
## RIGHT. Always use properly namespaced root for your configuration.
my.company.httpServer {
  // config keys here
}
```

## Convention over configuration
Either you are writing very simple module with no configuration required, or extensive plugin with
more complicated config, you should always respect the rule *convention over configuration*. Don't
make the user of your module feel lost in dozens of config keys. This can be easily avoided by using
default configuration where suitable, when no configuration explicitly provided by modules's user.

### Example
As an example, let the following configuration represent the configuration of our fictional module,
providing simple *HTTP* server:

```
com.mycompany.httpServer {
    host = localhost
    port = 8080
    protocol = http     // http, https
    timeout = 300       // [seconds]
}
```

All of the above configuration keys can be set as *optional*, with defaults set to values the
module's user would probably expected.

### Summary
- Use default values for omitted config keys where suitable, respect the
  *convention over configuration* rule.
- Always properly document your module configuration, and default values in your module
  documentation.

## Configuring provided services
Very often your module will provide custom *services* to another modules by registering them into
the *Akkamo context*. As those services usually needs some configuration,
designing this configuration structure properly and in unified way will help adopt your module more
easily by other users.

### Example

As an example, let's extend previously shown fictional *HTTP* server module with possibility to
register multiple *HTTP* connections. Every configured connection will be registered into the
*Akkamo context* as an instance of service class `HttpConnection`. Below is example of well
structured, recommended way how to configure your services, with further explanation:

```
com.mycompany.httpServer {
    httpConnection {
        default = true
        aliases = [ "conn1", "http" ]
        host = localhost
        port = 8080
        protocol = http
    }
    httpsConnection {
        aliases = [ "conn2", "https" ]
        host = localhost
        port = 8443
        protocol = https
    }
}
```

So what's happening here? Inside the root of our configuration, `com.mycompany.httpServer`, two
configuration blocks are defined. Each configuration block represents the configuration of single
`HttpConnection` service. Name of the configuration block (e.g. `httpConnection`) represents the
*key*, under which the connection will be registered into the *Akkamo context* and under which will
be available for injection. Same function have the *aliases*, under which the connection is also
available for injection. Next, notice the `default` config key. When one of the configuration is
marked as *default*, it is registered and can be injected without explicitly specifying the *key*.
Note that it should be always possible to mark only one configuration as *default*. When only one
configuration is present, it should be also considered as *default* without explicitly specifying
the `default` key.

Based on the recommendations and configuration example above, let's show how our configured services
can be injected:

- *httpConnection*
  - using its name (i.e. `httpConnection`): `ctx.get[HttpConnection]("httpConnection")`
  - using one of the aliases: `ctx.get[HttpConnection]("http")`
  - injectig as *default*: `ctx.get[HttpConnection]`
- *httpsConnection*
  - using its name (i.e. `httpsConnection`): `ctx.get[HttpConnection]("httpsConnection")`
  - using one of the aliases: `ctx.get[HttpConnection]("https")`

### Summary
- Use consistent style for registered services configuration among your modules.
- Each service should be registered into the *Akkamo context* under its name, and aliases.
- You should register also *default* service instance, for the configuration marked as *default*
  using the `default` config key, or implicitly if only one service configuration exists.
- Keep in mind that combination of service type (e.g. `HttpConnection`) and *key*
  (e.g. `httpConnection`) must be unique.

### Further reading
- [Akkamo Module](../how-it-works/module.md)
- [Akkamo Context](../how-it-works/context.md)