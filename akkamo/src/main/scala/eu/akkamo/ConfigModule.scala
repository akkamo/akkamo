package eu.akkamo

import com.typesafe.config.{Config, ConfigFactory}

/**
  * Module providing application-wide configuration, using the ''Typesafe Config'' library.
  * For further details about configuration syntax and usage, please refer to ''Typesafe Config''
  * homepage: [[https://github.com/typesafehub/config]].
  *
  * @author jubu
  */
class ConfigModule extends Module with Initializable with Publisher {

  /**
    * Initializes ''Typesafe Config'' configuration and registers into the ''Akkamo'' context.
    *
    * @param ctx ''Akkamo'' context
    * @return `true` if the module has been properly initialized
    */
  override def initialize(ctx: Context) = {
    ctx.register(ConfigFactory.load())
  }

  override def dependencies(dependencies: TypeInfoChain): TypeInfoChain = dependencies

  override def publish(dependency: TypeInfoChain): TypeInfoChain = dependency.&&[Config]
}
