package eu.akkamo.persistentconfig

import scala.concurrent.Future

/**
	* Generic persistent storage API
	*
	* @author jubu
	*/
trait Storage {

  sealed trait Result {

    def isFailure: Boolean

    def isSuccess: Boolean
  }

  case object Ok extends Result {

    override def isFailure = false

    override def isSuccess = true
  }

  case class Failure(th: Throwable) extends Exception(th) with Result {

    override def isFailure = true

    override def isSuccess = false
  }

  def getString(key: String): Future[Option[String]]

  def getInt(key: String): Future[Option[Int]]

  def getBoolean(key: String): Future[Option[Boolean]]

  def getLong(key: String): Future[Option[Long]]

  def getDouble(key: String): Future[Option[Double]]

  def getIntList(key: String): Future[Option[List[Int]]]

  def getStringList(key: String): Future[Option[List[String]]]

  def getLongList(key: String): Future[Option[List[Long]]]

  def getDoubleList(key: String): Future[Option[List[Double]]]


  def storeInt(key: String, value: Int): Future[Result]

  def storeString(key: String, value: String): Future[Result]

  def storeBoolean(key: String, value: Boolean): Future[Result]

  def storeLong(key: String, value: Long): Future[Result]

  def storeDouble(key: String, value: Double): Future[Result]

  def storeIntList(key: String, value: List[Int]): Future[Result]

  def storeStringList(key: String, value: List[String]): Future[Result]

  def storeLongList(key: String, value: List[Long]): Future[Result]

  def storeDoubleList(key: String, value: List[Double]): Future[Result]

	/**
		* Remove value under key
		*
		* @param key
		* @return
		*/
  def remove(key:String): Future[Result]
}


trait StorageHolder {

  def storage:Storage
}
