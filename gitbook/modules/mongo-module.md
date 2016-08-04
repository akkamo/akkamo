# Mongo module

This module provides support for the [MongoDB](https://www.mongodb.com) database, using the official
[Mongo Scala driver](https://github.com/mongodb/mongo-scala-driver).

> **Info**
  If you are interested in using
  the [ReactiveMongo](http://reactivemongo.org) Scala driver instead, please refer the
  [ReactiveMongo Module](reactivemongo-module.md) document.

## Module configuration
This module requires its configuration under the `akkamo.mongo` namespace. Each configuration block
under this namespace represents the configuration of single MongoDB connection and will be
registered into the *Akkamo context* as the instance of `MongoApi` trait.

### Configuration keys
As all built-in *Akkamo* modules, this module follows the
[configuration best practices](../best-practices/module-config.md). Please refer this chapter for
further details. Below are listed all available configuration keys:

- `aliases` *(optional)* - defines the array of *alias names*, under which the connection will be
  registered to the *Akkamo* context
- `default` *(optional)* - `true/false` whether the connection will be available for injection as
  *default* (Please note that only one configured connection can be specified as *default*)
- `uri` - MongoDB URI (see
   [MongoDB documentation](https://docs.mongodb.com/manual/reference/connection-string/) for more
   details)

Example, fully working configuration is shown below:

```
akkamo.mongo {
  conn1 {   // MongoDB connection with name 'conn1'
    aliases = ["alias1", "alias2"]    // aliases for the connection 'conn1'
    default = true  // marks that this connection is the default one
    uri = "mongodb://someuser:somepasswd@localhost:27017/your_db1_name"   // MongoDB URI
  }

  conn2 {   // MongoDB connection with name 'conn2'
    aliases = ["alias3"]    // aliases for the connection 'conn1'
    uri = "mongodb://someuser:somepasswd@localhost:27017/your_db1_name"   // MongoDB URI
  }
}
```

The above configuration creates two MongoDB connections, one with name *conn1*, aliases *alias1* and
*alias2* and also marked as the *default* connection, second with name *conn2* and alias *alias3*.

## How to use in your module
Each configured connection is registered into the *Akkamo context* and available for injection,
using the following rules:

- **inject by the configuration name**  
  Selected connection can be injected using its configuration name, e.g.
  `ctx.inject[MongoApi]("conn1")`
- **inject the default connection**  
  If any configured connection has set the `default = true` property, it will be considered as
  *default* connection and can be injected without specifying the key, e.g.
  `ctx.inject[MongoApi]`. Please note that only one configured connection can be specified
  as *default*.
- **inject by the name alias**  
  Besides the configuration name, each configured connection can be identified by one or more
  *alias name*. Such *alias name* can be used to inject the selected connection, e.g.
  `ctx.inject[MongoApi]("alias3")`.

Below is example code of very simple module, injecting actor systems configured by configuration
example shown above.

```scala
class MyModule extends Module with Initializable {
  override def initialize(ctx: Context) = Try {
    // injects the connection marked as default ('conn1' in this case)
    val conn1: Option[MongoApi] = ctx.inject[MongoApi]

    // same as above, with explicitly specified name
    val conn1_1: Option[MongoApi] = ctx.inject[MongoApi]("conn1")

    // injects the connection 'conn2', which has defined the alias 'alias3'
    val conn2: Option[MongoApi] = ctx.inject[MongoApi]("alias3")

    // ... rest of method code ...

  }

  // don't forget to add MongoModule module dependency to your module
  override def dependencies(dependencies: Dependency): Dependencies =
    dependencies.&&[MongoModule]
}
```

## Provided APIs
This module registers into the *Akkamo context* following types:

- `MongoApi`  
  For each configured MongoDB connection, instance of this trait will be registered into the
  *Akkamo context*. This trait provides following fields:
  - `client` - a client-side representation of the configured *MongoDB* cluster. See the
  [official Scaladoc](https://mongodb.github.io/mongo-scala-driver/1.1/scaladoc/#org.mongodb.scala.MongoClient)
  documentation.
  - `db` - representation of the database, allowing to access its collections. See the
  [official Scaladoc](https://mongodb.github.io/mongo-scala-driver/1.1/scaladoc/#org.mongodb.scala.MongoDatabase)
  documentation.

## Module dependencies
This module depends on following core modules:

- [Config module](config-module.md)
- [Log module](log-module.md)