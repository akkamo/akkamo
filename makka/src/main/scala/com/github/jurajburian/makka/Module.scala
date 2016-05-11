package com.github.jurajburian.makka

/**
	* Marking interface denotes an module
	*
	* @author jubu
	*/
trait Module {
	/**
		* @return simple class name
		*/
	override def toString: String = this.getClass.getSimpleName
}