package eu.akkamo.persistentconfig

import com.typesafe.config.Config
import eu.akkamo.{TypeInfoChain, Initializable, Module, Publisher}

import scala.concurrent.Future

trait ConfigHolder {
  def cfg: Config
}

/**
  * Represents the service registered into the ''Akkamo context'' by this module, provides methods
  * for reading, adding and deleting persistent properties.
  */
trait PersistentConfig {
  self: StorageHolder with ConfigHolder =>

  type Reader[T] = (Storage, Config, String) => Future[T]
  type Writer[T] = (Storage, String, T) => Future[Unit]

  /**
    * Reads value of the persistent property, specified by its ''alias''.
    *
    * @param key property alias
    * @param r   implicit property reader
    * @tparam T type of the property value
    * @return property value
    */
  def get[T](key: String)(implicit r: Reader[T]): Future[T] = {
    r(storage, cfg, key)
  }

  /**
    * Stores the persistent property ''value'' using the specified ''alias''.
    *
    * @param key    property alias
    * @param value  property value
    * @param writer implicit property writer
    * @tparam T type of the property value
    * @return operation result
    */
  def store[T](key: String, value: T)(implicit writer: Writer[T]): Future[Unit] = {
    writer(storage, key, value)
  }

  /**
    * Removes the persistent property, specified by its ''alias''.
    *
    * @param key property alias
    * @return operation result
    */
  def remove(key: String): Future[Unit] = {
    storage.remove(key)
  }
}

/**
  * Exception thrown when error reading/writing persistent property occurs.
  *
  * @param message   details message
  * @param throwable optional exception cause
  */
case class PersistentConfigException(message: String, throwable: Throwable = null)
  extends RuntimeException(message, throwable)

/**
  * Trait representing the ''Persistent config'' module. Actual implementations can use various
  * technologies for persistent storage, such as ''MongoDB'', SQL database, etc.
  *
  * @author jubu
  */
trait PersistentConfigModule extends Module with Initializable with Publisher {

  override def publish(ds: TypeInfoChain): TypeInfoChain = ds.&&[PersistentConfig]
}
