package eu.akkamo.persistentconfig

import scala.concurrent.Future

/**
	* Generic persistent storage API
	*
	* @author jubu
	*/
trait Storage {

  def getString(key: String): Future[Option[String]]

  def getInt(key: String): Future[Option[Int]]

  def getBoolean(key: String): Future[Option[Boolean]]

  def getLong(key: String): Future[Option[Long]]

  def getDouble(key: String): Future[Option[Double]]

  def getIntList(key: String): Future[Option[List[Int]]]

  def getStringList(key: String): Future[Option[List[String]]]

  def getLongList(key: String): Future[Option[List[Long]]]

  def getDoubleList(key: String): Future[Option[List[Double]]]


  def storeInt(key: String, value: Int): Future[Unit]

  def storeString(key: String, value: String): Future[Unit]

  def storeBoolean(key: String, value: Boolean): Future[Unit]

  def storeLong(key: String, value: Long): Future[Unit]

  def storeDouble(key: String, value: Double): Future[Unit]

  def storeIntList(key: String, value: List[Int]): Future[Unit]

  def storeStringList(key: String, value: List[String]): Future[Unit]

  def storeLongList(key: String, value: List[Long]): Future[Unit]

  def storeDoubleList(key: String, value: List[Double]): Future[Unit]

	/**
		* Remove value under key
		*
		* @param key
		* @return
		*/
  def remove(key:String): Future[Unit]
}


trait StorageHolder {

  def storage:Storage
}
