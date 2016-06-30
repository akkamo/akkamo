package eu.akkamo.persistentconfig

import com.typesafe.config.Config
import eu.akkamo.{Initializable, Module}

import scala.concurrent.Future

trait ConfigHolder {
  def cfg: Config
}

/**
  * Persistent config provides management of persistent properties
  */
trait PersistentConfig {
  self: StorageHolder with ConfigHolder =>

  type Reader[T] = (Storage, Config, String) => Future[T]
  type Writer[T] = (Storage, String, T) => Future[Storage#Result]

  def get[T](key: String)(implicit r: Reader[T]): Future[T] = {
    r(storage, cfg, key)
  }

  def store[T](key: String, value: T)(implicit writer: Writer[T]) = {
    writer(storage, key, value)
  }

  def remove(key: String) = {
    storage.remove(key)
  }
}

case class PersistentConfigException(message: String, throwable: Throwable = null) extends RuntimeException(message, throwable)


/**
  * @author jubu
  */
trait PersistentConfigModule extends Module with Initializable


