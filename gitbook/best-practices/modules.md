# Modules - Best Practices
*Akkamo platform* opens the possibility to write custom modules for various different technologies,
frameworks and libraries. Anyone can contribute with own module, which can be handy for other
people. Although every developer has his/her own coding style, keeping the following recommendations
when writing *Akkamo modules* may simplify adoption your module by another developer, and you might
find adopting someone's module easier too.

## Module configuration

### Basic recommendations
*Akkamo platform* already provides mechanisms for loading configuration for your custom module, via
the following built-in modules:

- [Config module](../modules/config-module.md) - provides support for non-persistable, readonly
  configuration, using the [Typesafe Config](https://github.com/typesafehub/config) library
- [Persistent config module](../modules/persistent-config-module.md) - provides support for
  persistable configuration, with default implementation using the
  [MongoDB](https://www.mongodb.com) database as persistent storage

Regardless of the selected configuration mechanism, you should keep in ming the following
recommendations when designing your configuration structure:

#### Use namespaces
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

### Convention over configuration
Either you are writing very simple module with no configuration required, or extensive plugin with
more complicated config, you should always respect the rule *convention over configuration*. Don't
make the user of your module feel lost in dozens of config keys. This can be easily avoided by using
default configuration where suitable, when no configuration explicitly provided by modules's user.

#### Example
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

#### Summary
- use default values for omitted config keys where suitable, respect the
  *convention over configuration* rule
- always properly document your module configuration, and default values in your module
  documentation