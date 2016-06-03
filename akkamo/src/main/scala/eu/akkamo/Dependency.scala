package eu.akkamo

import scala.reflect.ClassTag

/**
	* @author jubu
	*/
trait Dependency {
	def &&[T<:(Module with Initializable)](implicit ct:ClassTag[T]):Dependency
	def apply() = res
	def res:Boolean
}
