package eu.akkamo.persistentconfig

import com.typesafe.config.Config
import eu.akkamo.{Initializable, Module, Publisher}

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
  type Writer[T] = (Storage, String, T) => Future[Storage#Result]

  /**
    * Reads value of the persistent property, specified by its ''key''.
    *
    * @param key property key
    * @param r   implicit property reader
    * @tparam T type of the property value
    * @return property value
    */
  def get[T](key: String)(implicit r: Reader[T]): Future[T] = {
    r(storage, cfg, key)
  }

  /**
    * Stores the persistent property ''value'' using the specified ''key''.
    *
    * @param key    property key
    * @param value  property value
    * @param writer implicit property writer
    * @tparam T type of the property value
    * @return operation result
    */
  def store[T](key: String, value: T)(implicit writer: Writer[T]): Future[Storage#Result] = {
    writer(storage, key, value)
  }

  /**
    * Removes the persistent property, specified by its ''key''.
    *
    * @param key property key
    * @return operation result
    */
  def remove(key: String): Future[Storage#Result] = {
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

  override def publish(): Set[Class[_]] = Set(classOf[PersistentConfig])
}
