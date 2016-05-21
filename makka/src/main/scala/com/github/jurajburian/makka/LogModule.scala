package com.github.jurajburian.makka

import akka.actor.ActorSystem
import akka.event.{Logging, LoggingAdapter}

/**
 * factory provided by `LogModule`
 *
 * @author jubu
 */
trait LoggingAdapterFactory {

	/** ?
	 *
	 * @param category
	 * @tparam T
	 * @return concrete `LoggingAdapter` for
	 */
	def apply[T](category:Class[T]):LoggingAdapter

	/** ?
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

	/* Module name ? */
	val LoggingActorSystem = "logging"

	/** Initializes log module into provided context */
	override def initialize(ctx:Context):Boolean = {
		if (ctx.initialized[AkkaModule]) {
			val actorSystem = ctx.inject[ActorSystem](LoggingActorSystem).getOrElse(throw InitializationError("Can't find any Actor System for logger"))
			//TODO is there really a neccessity of having logging factory when there
			//will always be one instance of LogModule in MAKKA?
			/* register logger into context */
			ctx.register[LoggingAdapterFactory](new LoggingAdapterFactory {
				override def apply[T](category:Class[T]):LoggingAdapter = Logging(actorSystem, category)
				override def apply(category:AnyRef):LoggingAdapter = apply(category.getClass)
			})
			true
		} else {
			false
		}
	}

}
