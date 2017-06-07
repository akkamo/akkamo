package eu.akkamo

/**
  * @author jubu
  */
trait LogModule extends Module with Initializable with Publisher {
  override def publish(ds: TypeInfoChain): TypeInfoChain = ds.&&[LoggingAdapterFactory]
}

