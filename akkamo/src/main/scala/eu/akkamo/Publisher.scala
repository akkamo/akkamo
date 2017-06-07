package eu.akkamo

/**
  * Defines interface for modules that publish an set of interfaces.
  * In case that the published interface is defined one can use defined interface as dependency.
  * <br/>
  * For example AkkaModule publish ActorSystem
  *
  * @author jubu.
  */
trait Publisher {

  /**
    *
    * @param ds instance of [[eu.akkamo.TypeInfoChain]]
    * @return chained module dependencies
    */
  def publish(ds:TypeInfoChain):TypeInfoChain
}
