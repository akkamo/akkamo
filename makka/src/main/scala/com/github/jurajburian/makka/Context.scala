package com.github.jurajburian.makka

//FIXME general ct is not a good variable name for classTag, tag would suit better

/**
	* A mutable context dispatched during all phases in module lifecycle
	*
	* @author jubu
	*/
trait Context {

	import scala.reflect.ClassTag

	trait With {
		def &&[T<:(Module with Initializable)](implicit ct:ClassTag[T]):With
		def apply() = res
		def res:Boolean
	}

	/**
	 * inject registered service into this context
	 *
	 * @param ct
	 * @tparam T
	 * @return implementation of interface `T` if initialized
	 */
	def inject[T](implicit ct:ClassTag[T]):Option[T]

	/**
	 * inject registered service into this context
	 *
	 * @param key additional mapping identifier
	 * @param strict - if true, only named instance is returned if false then
	 *                 when nothing found under key then default instance is
	 *                 returned if it exists and is initialized
	 * @param ct class tag evidence
	 * @tparam T
	 * @return implementation of interface `T` if initialized
	 */
	def inject[T](key:String, strict:Boolean = false)(implicit ct:ClassTag[T]):Option[T]

	/**
	 * register bean
	 *
	 * @param value
	 * @param key
	 * @param ct class tag evidence
	 * @tparam T
	 */
	def register[T<:AnyRef](value:T, key:Option[String] = None)(implicit ct:ClassTag[T])

	//TODO same can be acchieved by calling initializedWith + apply() on result
	//TODO maybe unneccessary bloating of code, (perf gain ? )
	/**
	 *
	 * @param ct class tag evidence
	 * @tparam T
	 * @return true if module is initialized
	 */
	def initialized[T<:(Module with Initializable)](implicit ct:ClassTag[T]):Boolean

	/**
		*
		* @param ct class tag evidence
		* @tparam T
		* @return [[With]] instance
		*/
	def initializedWith[T<:(Module with Initializable)](implicit ct:ClassTag[T]):With

	//TODO same can be acchieved by calling initializedWith + apply() on result
	//TODO maybe unneccessary bloating of code, (perf gain ? )
	/**
	 *
	 * @param ct class tag evidence
	 * @tparam T
	 */
	def running[T<:(Module with Runnable)](implicit ct:ClassTag[T]):Boolean

	/**
	 *
	 * @param ct class tag evidence
	 * @tparam T
	 * @return
	 */
	def runningWith[T<:(Module with Initializable)](implicit ct:ClassTag[T]):With

}
