package com.github.jurajburian.makka

import akka.actor.ActorSystem
import akka.event.{Logging, LoggingAdapter}
import com.typesafe.config.Config

import scala.util.Try

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

	val LoggingActorSystem = "akka.logging.actorSystem"

	override def initialize(ctx: Context): Boolean = ctx.inject[Config] match {
		case Some(cfg) => {
			val actorSystem = Try(cfg.getString(LoggingActorSystem)).toOption.map(ctx.inject[ActorSystem](_)).getOrElse(ctx.inject[ActorSystem])
			actorSystem match {
				case Some(as)=> {
					ctx.register[LoggingAdapterFactory](new LoggingAdapterFactory {
						override def apply[T](category: Class[T]): LoggingAdapter = Logging(as, category)
						override def apply(category: AnyRef): LoggingAdapter = Logging(as, category.getClass)
					})
					true
				}
				case _ => false // actor system is not configured, postpone initialization
			}
		}
		case _ => false // no config postpone initialization
	}
}
