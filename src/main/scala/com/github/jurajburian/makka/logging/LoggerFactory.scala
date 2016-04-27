package com.github.jurajburian.makka.logging

import akka.event.{LoggingAdapter}

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
