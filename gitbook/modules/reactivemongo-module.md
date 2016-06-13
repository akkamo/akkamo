# ReactiveMongo module

This module provides support for the [MongoDB](https://www.mongodb.com) database, [ReactiveMongo](http://reactivemongo.org) Scala driver.

## Module configuration
Example, fully working configuration is shown below:

```
akkamo.reactiveMongo {
  conn1 {   // MongoDB connection with name 'conn1'
    aliases = ["alias1", "alias2"]    // aliases for the connection 'conn1'
    default = true  // marks that this connection is the default one
    uri = "mongodb://someuser:somepasswd@localhost:27017/your_db1_name"   // MongoDB URI
    mongo-async-driver {  // configuration of the ReactiveMongo's underlying Akka system
      akka {
        loglevel = WARNING
      }
    }
  }

  conn2 {   // MongoDB connection with name 'conn2'
    aliases = ["alias3"]    // aliases for the connection 'conn1'
    uri = "mongodb://someuser:somepasswd@localhost:27017/your_db1_name"   // MongoDB URI
    mongo-async-driver {  // configuration of the ReactiveMongo's underlying Akka system
      akka {
        loglevel = WARNING
      }
    }
  }
}
```

The above configuration creates two MongoDB connections, one with name *conn1*, aliases *alias1* and *alias2* and also marked as the *default* connection, second with name *conn2* and alias *alias3*.

Each configured connection is represented by the `eu.akkamo.akkamo.mongo.ReactiveMongoApi` trait, which has following methods:
- `driver: reactivemongo.api.MongoDriver` - reference to the *ReactiveMongo* driver for the configured connection (see [Scaladoc](http://reactivemongo.org/releases/0.11/api/#reactivemongo.api.MongoDriver))
- `connection: reactivemongo.api.MongoConnection` - reference to the *ReactiveMongo* connection itself (see [Scaladoc](http://reactivemongo.org/releases/0.11/api/#reactivemongo.api.MongoConnection))
- `db: reactivemongo.api.DB` - reference to the *ReactiveMongo* database (see [Scaladoc](http://reactivemongo.org/releases/0.11/api/#reactivemongo.api.DB))

> Please note that if the configuration is incorrect or MongoDB is not running at time of module initialization, the entire Akkamo initialization process will be interrupted (_fail-fast_ principle).

## Usage in your module
Now you can inject configured MongoDB connections in your Akkamo module (for more info about creating Akkamo modules, please refer the [Akkamo documentation](https://github.com/akkamo/akkamo/)) for example in following way:

```scala
class MyModule extends Module with Initializable {
  override def initialize(ctx: Context): Unit = {
    // injects the connection marked as default ('conn1' in this case)
    val conn1: Option[ReactiveMongoApi] = ctx.inject[ReactiveMongoApi]

    // same as above, with explicitly specified name
    val conn1_1: Option[ReactiveMongoApi] = ctx.inject[ReactiveMongoApi]("conn1")

    // injects the connection 'conn2', which has defined the alias 'alias3'
    val conn2: Option[ReactiveMongoApi] = ctx.inject[ReactiveMongoApi]("alias3")

    // ... rest of method code ...

  }

  // don't forget to add ReactiveMongo module dependency to your module
  override def dependencies(dependencies: Dependency): Dependencies =
    dependencies.&&[ReactiveMongoModule]
}
```

## Module dependencies
This module depends on following core modules:

* [Akka module](akka-module.md)
* [Config module](config-module.md)
* [Log module](log-module.md)
