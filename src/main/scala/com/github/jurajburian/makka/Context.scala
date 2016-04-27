package com.github.jurajburian.makka

/**
	* Context dispatched during all phases in module lifecycle
	* @author jubu
	*/
trait Context {
	import scala.reflect.runtime.universe.TypeTag

	/**
		* inject service
		* @param tt
		* @tparam T require
		* @return implementation of interface `T` if initialized
		*/
	def inject[T](implicit tt: TypeTag[T]):Option[T]

	/**
		* inject service
		* @param key additional mapping identifier
		* @tparam T
		* @return implementation of interface `T` if initialized
		*/
	def inject[T](key:String)(implicit tt: TypeTag[T]):Option[T]

	/**
		*
		* @param value
		* @param key
		* @param tt
		* @tparam T
		*/
	def register[T<:AnyRef](value:T, key:Option[String] = None)(implicit tt: TypeTag[T])
}
