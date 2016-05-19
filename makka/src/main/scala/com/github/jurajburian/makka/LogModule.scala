package com.github.jurajburian.makka

import akka.actor.ActorSystem
import akka.event.{Logging, LoggingAdapter}

/**
	* factory provided by `LogModule`
	* @author jubu
	*/
trait LoggingAdapterFactory {

	/**
		*
		* @param category
		* @return concrete `LoggingAdapter` for
		*/
	def apply[T](category:Class[T]):LoggingAdapter


	/**
		*
		* @param category
		* @return concrete `LoggingAdapter` for
		*/
	def apply(category:AnyRef):LoggingAdapter
}



/**
	* Provides logger outside of Actors world
	*
	* @author jubu
	*/
class LogModule extends Module with Initializable {

	val LoggingActorSystem = "logging"

	override def initialize(ctx: Context): Boolean = {
		if(ctx.initialized[AkkaModule]) {
			val actorSystem = ctx.inject[ActorSystem](LoggingActorSystem).getOrElse(throw InitializationError("Can't find any Actor Suystem for logger"))
			ctx.register[LoggingAdapterFactory](new LoggingAdapterFactory {
				override def apply[T](category: Class[T]): LoggingAdapter = Logging(actorSystem, category)
				override def apply(category: AnyRef): LoggingAdapter = apply(category.getClass)
			})
			true
		} else {false}
	}
}
