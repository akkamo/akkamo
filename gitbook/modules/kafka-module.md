# Kafka module
This module provides support for the [Apache Kafka](http://kafka.apache.org), a high throughput
distributed messaging system.

## Module configuration
This module requires its configuration under the `akkamo.kafka` namespace. Each configuration block
under this namespace represents the configuration of single Kafka connection and will register into
the *Akkamo context* instance of
[KafkaProducer](https://kafka.apache.org/090/javadoc/index.html?org/apache/kafka/clients/producer/KafkaProducer.html)
and/or
[KafkaConsumer](https://kafka.apache.org/090/javadoc/index.html?org/apache/kafka/clients/consumer/KafkaConsumer.html)
, depending on the module configuration.

### Configuration keys
- `aliases` *(optional)* - defines the array of *alias names*, under which the Kafka
  producer/consumer will be registered to the *Akkamo* context
- `default` *(optional)* - `true/false` whether the Kafka producer/consumer will be available for
  injection as *default* (Please note that only one configured connection can be specified as
  *default*)
- `properties` - path to the Kafka *properties file* (see
  [official documentation](http://kafka.apache.org/documentation.html#configuration) for more info)
- `producer` *(optional)* - whether the Kafka producer should be registered into the
  *Akkamo context* for the particular configuration (default is `false`)
- `consumer` *(optional)* - whether the Kafka consumer should be registered into the
  *Akkamo context* for the particular configuration (default is `false`)

Example of fully working configuration is shown below:

```
akkamo.kafka = {
  // complete configuration with several name aliases
  name1 = {
    aliases = ["alias1", "alias2"]
    properties = "name1.properties" // path to property file
    producer = true
    consumer = true
  },
  // configuration registered as default (only one instance is allowed)
  name2 = {
    default = true
    properties = "name2.properties"
    consumer = true
  }
}
```

The above configuration creates two Kafka configurations. First configuration causes that Kafka
producer and consumer will be registered into the *Akkamo context* under the name `name1` and
aliases `alias1` and `alias2`, Kafka configuration itself will be loaded from properties file called
`name1.properties`. Second configuration causes that only Kafka consumer will be registered into the
*Akkamo* context and will be available for injection as *default* service (e.g. without need to
specify any *alias*) and under its name `name2`, Kafka configuration itself will be loaded from
properties file called `name2.properties`

## How to use in your module
Each configured connection registers into the *Akkamo context* either the Kafka producer or consumer
or eventually both of them, and those are available for injection using the following rules:

- **inject by the configuration name**  
  Selected Kafka producer/consumer can be injected using its configuration name, e.g.
  `ctx.inject[KafkaProducer[String, String]]("name1")`
- **inject the default connection**  
  If any configuration has set the `default = true` property, it will be considered as
  *default* and can be injected without specifying the alias, e.g.
  `ctx.inject[KafkaProducer[String, String]]`. Please note that only one configuration can be
  marked as *default*.
- **inject by the name alias**  
  Besides the configuration name, each configured Kafka producer/consumer can be identified by one
  or more *alias name*. Such *alias name* can be used to inject the selected connection, e.g.
  `ctx.inject[KafkaConsumer[String, String]]("alias2")`.

Below is example code of very simple module, injecting Kafka producer/consumer configured by
configuration example shown above.

```scala
class MyModule extends Module with Initializable {
  override def initialize(ctx: Context) = Try {
    // injects the Kafka consumer marked as default ('name2' configuration in this case)
    val consumerD: Option[KafkaConsumer[String, String]] =
      ctx.inject[KafkaConsumer[String, String]]

    // same as above, with explicitly specified name
    val consumerD_1: Option[KafkaConsumer[String, String]] =
      ctx.inject[KafkaConsumer[String, String]]("name2")

    // injects the Kafka consumer registered with name 'name1'
    val consumer: Option[KafkaConsumer[String, String]] =
      ctx.inject[KafkaConsumer[String, String]]("alias1")

    // ... rest of method code ...

  }

  // don't forget to add Kafka module dependency to your module
  override def dependencies(dependencies: Dependency): Dependencies =
    dependencies.&&[KafkaModule]
}
```

## Provided APIs
This module registers into the *Akkamo* context following services:

- `KafkaProducer` - see [official Javadoc](https://kafka.apache.org/090/javadoc/index.html?org/apache/kafka/clients/producer/KafkaProducer.html)
  for more details
- `KafkaConsumer` - see [official Javadoc](https://kafka.apache.org/090/javadoc/index.html?org/apache/kafka/clients/consumer/KafkaConsumer.html)
  for more details
  
## Module dependencies
This module depends on following core modules:

* [Config module](config-module.md)
* [Log module](log-module.md)