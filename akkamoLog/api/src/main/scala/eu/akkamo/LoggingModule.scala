package eu.akkamo

/**
  * @author jubu
  */
trait LogModule extends Module with Initializable with Publisher {

  override def publish(): Set[Class[_]] = Set(classOf[LoggingAdapterFactory])
}

