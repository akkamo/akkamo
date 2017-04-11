package eu.akkamo

import akka.actor.ActorSystem
import akka.event.{Logging}
import eu.akkamo.LoggingAdapter.LogLevel

import scala.util.Try

/**
  * This module provides `eu.akkamo.LoggingAdapterFactory` via the ''Akkamo'' context, allowing to use
  * the configured logging system outside ''Akka'' actors.
  *
  * @author jubu
  * @see LoggingAdapterFactory
  */
class AkkaLogModule extends LogModule {

  /**
    * Name of the ''Akka'' actor system used for the logging. If no such actor system is found,
    * the default one is used.
    */
  val LoggingActorSystem = this.getClass.getName

  /** Initializes log module into provided context */
  override def initialize(ctx: Context) = Try {
    // inject the logging actor system (if available, otherwise default actor system)
    val actorSystem = ctx.inject[ActorSystem](LoggingActorSystem)
      .getOrElse(throw InitializableError("Can't find any Actor System for logger"))

    // register logging adapter factor into the Akkamo context
    ctx.register[LoggingAdapterFactory](new LoggingAdapterFactory {

      override def apply[T](category: Class[T]): LoggingAdapter = new LoggingAdapter {

        val delegate = Logging(actorSystem, category)

        implicit val tr = (p:LogLevel) => Logging.LogLevel.apply(p.asInt)

        override def log(level: LogLevel, message: String) = delegate.log(level, message)

        override def log(level: LogLevel, template: String, arg1: Any) = delegate.log(level, template, arg1)

        override def log(level: LogLevel, template: String, arg1: Any, arg2: Any) = delegate.log(level, template, arg1, arg2)

        override def log(level: LogLevel, template: String, arg1: Any, arg2: Any, arg3: Any) = delegate.log(level, template, arg1, arg2, arg3)

        override def log(level: LogLevel, template: String, arg1: Any, arg2: Any, arg3: Any, arg4: Any) = delegate.log(level, template, arg1, arg2, arg3, arg4)

        override def isErrorEnabled = delegate.isErrorEnabled

        override def isInfoEnabled = delegate.isInfoEnabled

        override def warning(message: String) = delegate.warning(message)

        override def warning(template: String, arg1: Any) = delegate.warning(template, arg1)

        override def warning(template: String, arg1: Any, arg2: Any) = delegate.warning(template, arg1, arg2)

        override def warning(template: String, arg1: Any, arg2: Any, arg3: Any) = delegate.warning(template, arg1, arg2, arg3)

        override def warning(template: String, arg1: Any, arg2: Any, arg3: Any, arg4: Any) = delegate.warning(template, arg1, arg2, arg3, arg4)

        override def isEnabled(level: LogLevel) = delegate.isEnabled(level)

        override def isDebugEnabled = delegate.isDebugEnabled

        override def error(cause: Throwable, message: String) = delegate.error(cause, message)

        override def error(cause: Throwable, template: String, arg1: Any) = delegate.error(cause, template, arg1)

        override def error(cause: Throwable, template: String, arg1: Any, arg2: Any) = delegate.error(cause, template, arg1, arg2)

        override def error(cause: Throwable, template: String, arg1: Any, arg2: Any, arg3: Any) = delegate.error(cause, template, arg1, arg2, arg3)

        override def error(cause: Throwable, template: String, arg1: Any, arg2: Any, arg3: Any, arg4: Any) = delegate.error(cause, template, arg1, arg2, arg3, arg4)

        override def error(message: String) = delegate.error(message)

        override def error(template: String, arg1: Any) = delegate.error(template, arg1)

        override def error(template: String, arg1: Any, arg2: Any) = delegate.error(template, arg1, arg2)

        override def error(template: String, arg1: Any, arg2: Any, arg3: Any) = delegate.error(template, arg1, arg2, arg3)

        override def error(template: String, arg1: Any, arg2: Any, arg3: Any, arg4: Any) = delegate.error(template, arg1, arg2, arg3, arg4)

        override def debug(message: String) = delegate.debug(message)

        override def debug(template: String, arg1: Any) = delegate.debug(template, arg1)

        override def debug(template: String, arg1: Any, arg2: Any) = delegate.debug(template, arg1, arg2)

        override def debug(template: String, arg1: Any, arg2: Any, arg3: Any) = delegate.debug(template, arg1, arg2, arg3)

        override def debug(template: String, arg1: Any, arg2: Any, arg3: Any, arg4: Any) = delegate.debug(template, arg1, arg2, arg3, arg4)

        override def isWarningEnabled = delegate.isWarningEnabled

        override def info(message: String) = delegate.info(message)

        override def info(template: String, arg1: Any) = delegate.info(template, arg1)

        override def info(template: String, arg1: Any, arg2: Any) = delegate.info(template, arg1, arg2)

        override def info(template: String, arg1: Any, arg2: Any, arg3: Any) = delegate.info(template, arg1, arg2, arg3)

        override def info(template: String, arg1: Any, arg2: Any, arg3: Any, arg4: Any) = delegate.info(template, arg1, arg2, arg3, arg4)

        override def format(t: String, arg: Any*) = delegate.format(t, arg:_*)
      }

      override def apply(category: AnyRef): LoggingAdapter = apply(category.getClass)
    })
  }

  override def dependencies(dependencies: Dependency): Dependency = dependencies.&&[AkkaModule]



  override def iKey() = classOf[LogModule]
}
