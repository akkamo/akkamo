package com.github.jurajburian.makka

import akka.actor.ActorSystem
import akka.event.{Logging, LoggingAdapter}

/**
	* Simple factory, providing `LoggingAdapter` instance for specified category (e.g. module name,
	* selected class). This factory is registered by the [[LogModule]] into the ''Makka'' context.
	*
	* @author jubu
	*/
trait LoggingAdapterFactory {

	/**
		* Returns instance of `LoggingAdapter` for specified category (e.g. module name,
		* selected class).
		*
		* @param category category for which the `LoggingAdapter` will be returned
		* @tparam T type of the category class
		* @return instance of `LoggingAdapter`
		*/
	def apply[T](category: Class[T]): LoggingAdapter

	/**
		* Returns instance of `LoggingAdapter` for specified category (e.g. module name,
		* selected class).
		*
		* @param category category for which the `LoggingAdapter` will be returned
		* @return instance of `LoggingAdapter`
		*/
	def apply(category: AnyRef): LoggingAdapter
}

/**
	* This module provides [[LoggingAdapterFactory]] via the ''Makka'' context, allowing to use
	* the configured logging system outside ''Akka'' actors.
	*
	* @author jubu
	* @see LoggingAdapterFactory
	*/
class LogModule extends Module with Initializable {

	/**
		* Name of the ''Akka'' actor system used for the logging. If no such actor system is found,
		* the default one is used.
		*/
	val LoggingActorSystem = "logging"

	/** Initializes log module into provided context */
	override def initialize(ctx: Context): Boolean = {
		if (ctx.initialized[AkkaModule]) {
			// inject the logging actor system (if available, otherwise default actor system)
			val actorSystem = ctx.inject[ActorSystem](LoggingActorSystem)
				.getOrElse(throw InitializationError("Can't find any Actor System for logger"))

			// register logging adapter factor into the Makka context
			ctx.register[LoggingAdapterFactory](new LoggingAdapterFactory {
				override def apply[T](category: Class[T]): LoggingAdapter = Logging(actorSystem, category)

				override def apply(category: AnyRef): LoggingAdapter = apply(category.getClass)
			})
			true
		} else {
			false
		}
	}
}
