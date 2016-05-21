package com.github.jurajburian.makka

//TODO please write in a few words "what is module" in this context

/**
 * Marking interface denotes an module
 *
 * @author jubu
 */
trait Module {

  /**
	 * @return simple class name in format ?
	 */
  override def toString:String = this.getClass.getSimpleName

}