package com.github.jurajburian.makka


/**
	* Context dispatched during all phases in module lifecycle
	*
	* @author jubu
	*/
trait Context {
	import scala.reflect.ClassTag

	trait With {
		def &&[T<:(Module with Initializable)](implicit ct: ClassTag[T]):With
		def apply() = res
		def res:Boolean
	}

	/**
		* inject registered service
		*
		* @param ct
		* @tparam T
		* @return implementation of interface `T` if initialized
		*/
	def inject[T](implicit ct: ClassTag[T]):Option[T]

	/**
		* inject registered service
		*
		* @param key additional mapping identifier
		* @param strict - if true, only named instance is returned
		*               if false then when nothing found under key then default instance is returned if exists and is initialized
		* @tparam T
		* @return implementation of interface `T` if initialized
		*/
	def inject[T](key:String, strict:Boolean = false)(implicit ct: ClassTag[T]):Option[T]

	/**
		* register bean
		*
		* @param value
		* @param key
		* @param ct
		* @tparam T
		*/
	def register[T<:AnyRef](value:T, key:Option[String] = None)(implicit ct: ClassTag[T])

	/**
		*
		* @param ct class tag evidence
		* @tparam T
		* @return true if module is initialized
		*/
	def initialized[T<:(Module with Initializable)](implicit ct: ClassTag[T]):Boolean

	/**
		*
		* @param ct
		* @tparam T
		* @return [[With]] instance
		*/
	def initializedWith[T<:(Module with Initializable)](implicit ct: ClassTag[T]):With


	/**
		*
		* @tparam T
		*/
	def running[T<:(Module with Runnable)](implicit ct: ClassTag[T]):Boolean

	/**
		*
		* @param ct
		* @tparam T
		* @return
		*/
	def runningWith[T<:(Module with Initializable)](implicit ct: ClassTag[T]):With
}
