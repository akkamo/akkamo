package com.github.jurajburian.makka

/**
	* Trait representing the ''Makka'' module, i.e. independent application unit, with possible
	* dependencies on another modules, providing own functionality to other modules. The main goal is
	* to allow writing applications as a set of modules, loosely coupled each other, allowing to
	* easily add new functionality to existing application (or remove it), or to prepare several
	* different application build profiles, with different functionality (modules) included.
	*
	* Each ''Makka'' module has its own lifecycle, with following stages: ''init stage''
	* ([[Initializable]] trait), ''run stage'' ([[Runnable]] trait) and ''dispose stage''
	* ([[Disposable]] trait). In order to execute desired module logic in selected stage,
	* corresponding trait must be mixed in.
	*
	* @author jubu
	* @see Initializable
	* @see Runnable
	* @see Disposable
	*/
trait Module {

	/**
		* Default implementation of `toString` of ''Makka'' module returns the module name itself
		* (simple class name).
		*
		* @return simple class name of the ''Makka'' module
		*/
	override def toString: String = this.getClass.getSimpleName
}