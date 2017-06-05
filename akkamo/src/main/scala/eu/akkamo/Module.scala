package eu.akkamo

/**
  * Trait representing the ''Akkamo'' module, i.e. independent application unit, with possible
  * dependencies on another modules, providing own functionality to other modules. The main goal is
  * to allow writing applications as a set of modules, loosely coupled each other, allowing to
  * easily add new functionality to existing application (or remove it), or to prepare several
  * different application build profiles, with different functionality (modules) included.
  *
  * Each ''Akkamo'' module has its own lifecycle, with following stages: ''init stage''
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
    * Default implementation of `toString` of ''Akkamo'' module returns the module name itself
    * (simple class name).
    *
    * @return simple class name of the ''Akkamo'' module
    */
  override def toString: String = this.getClass.getSimpleName

  /**
    * Each ''Akkamo'' module must override this method in order to '''exactly''' define its module
    * dependencies, otherwise there will be risk that it will not be initialized and run in proper
    * order. Fox example if this module depends on modules `ModuleA` and `ModuleB`, then the
    * overridden method should look like:
    *
    * {{{
    *   override def dependencies(dependencies: Dependency): Dependency =
    *     dependencies.&&[ModuleA].&&[ModuleB]
    * }}}
    *
    * Instead of modules, one can use interfaces that are published by given module, see: [[eu.akkamo.Publisher]] interface for more details
    *
    * @param ds instance of [[eu.akkamo.Dependency]]
    * @return chained module dependencies
    */
  def dependencies(ds: Dependency): Dependency


  /**
    * Instance of [[eu.akkamo.Module]] is registered into the ''Akkamo'' context by default under
    * this module class. Override this method in order to achieve different registration key
    * class, for example an interface instead of concrete implementation.
    *
    * @return registration key class
    */
  def iKey(): Class[_] = this.getClass

}
