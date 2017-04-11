package eu.akkamo.persistentconfig

import com.typesafe.config.Config
import eu.akkamo._
import eu.akkamo.mongo.{ReactiveMongoApi, ReactiveMongoModule}
import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.api.commands.WriteResult
import reactivemongo.bson.{BSONDocument, BSONDocumentReader, BSONDocumentWriter}

import scala.concurrent.Future
import scala.util.Try

case class IntProperty(val _id: String, val value: Int) extends Property[Int]

case class StringProperty(val _id: String, val value: String) extends Property[String]

case class LongProperty(val _id: String, val value: Long) extends Property[Long]

case class BooleanProperty(val _id: String, val value: Boolean) extends Property[Boolean]

case class DoubleProperty(val _id: String, val value: Double) extends Property[Double]

case class IntListProperty(val _id: String, val value: List[Int]) extends Property[List[Int]]

case class StringListProperty(val _id: String, val value: List[String]) extends Property[List[String]]

case class LongListProperty(val _id: String, val value: List[Long]) extends Property[List[Long]]

case class DoubleListProperty(val _id: String, val value: List[Double]) extends Property[List[Double]]

sealed trait Property[V]  {
  val _id: String
  val value: V
}


/**
  * @author jubu
  */
class MongoPersistentConfigModule extends PersistentConfigModule with Initializable {

  val cfgKey = "persistentConfig"

  override def initialize(ctx: Context) = Try {
    val register = build(ctx)
    ctx.register[PersistentConfig](register)
  }

  override def dependencies(dependencies: Dependency): Dependency =
    dependencies.&&[Config].&&[ReactiveMongoModule].&&[LoggingAdapter]

  def build(ctx: Context): PersistentConfig = {
    val api = ctx.inject[ReactiveMongoApi](cfgKey).getOrElse(throw InitializableError("Missing ReactiveMongoApi instance!"))

    val mongoStorage = new Storage {

      val collection = api.db.collection[BSONCollection]("config")

      implicit val ic = api.driver.system.dispatcher

      implicit object PropertyReader extends  BSONDocumentReader[Property[_]] {
        override def read(bson: BSONDocument) = {
         // bson.getAs[String]("className")
          // TODO
          BooleanProperty("", true)
        }
      }

      implicit object PropertyWriter extends  BSONDocumentWriter[Property[_]] {
        override def write(t: Property[_]) = {
          BSONDocument()
        }
      }



      /*
            implicit val pH: BSONDocumentReader[Property[_]] with BSONDocumentWriter[Property[_]]
              with BSONHandler[BSONDocument, Property[_]] = reactivemongo.bson.Macros.handlerOpts[Property[_], SaveSimpleName]
      */

      override def getString(key: String): Future[Option[String]] = find[String, StringProperty](key)

      override def getIntList(key: String): Future[Option[List[Int]]] = find[List[Int], IntListProperty](key)

      override def storeStringList(key: String, value: List[String]): Future[Result] = upsert(StringListProperty(key, value))

      override def storeLongList(key: String, value: List[Long]): Future[Result] = upsert(LongListProperty(key, value))

      override def storeBoolean(key: String, value: Boolean): Future[Result] = upsert(BooleanProperty(key, value))

      override def getLongList(key: String): Future[Option[List[Long]]] = find[List[Long], LongListProperty](key)

      override def getDouble(key: String): Future[Option[Double]] = find[Double, DoubleProperty](key)

      override def storeString(key: String, value: String): Future[Result] = upsert(StringProperty(key, value))

      override def getDoubleList(key: String): Future[Option[List[Double]]] = find[List[Double], DoubleListProperty](key)

      override def getLong(key: String): Future[Option[Long]] = find[Long, LongProperty](key)

      override def storeDouble(key: String, value: Double): Future[Result] = upsert(DoubleProperty(key, value))

      override def storeInt(key: String, value: Int): Future[Result] = upsert(IntProperty(key, value))

      override def storeDoubleList(key: String, value: List[Double]): Future[Result] = upsert(DoubleListProperty(key, value))

      override def storeLong(key: String, value: Long): Future[Result] = upsert(LongProperty(key, value))

      override def getBoolean(key: String): Future[Option[Boolean]] = find[Boolean, BooleanProperty](key)

      override def storeIntList(key: String, value: List[Int]): Future[Result] = upsert(IntListProperty(key, value))

      override def getStringList(key: String): Future[Option[List[String]]] = find[List[String], StringListProperty](key)

      override def getInt(key: String): Future[Option[Int]] = find[Int, IntProperty](key)

      /**
        * Remove value under key
        *
        * @param key
        * @return
        */
      override def remove(key: String): Future[Result] = {
        val selector = BSONDocument("_id" -> key)
        collection.remove(selector).transform(ok, failed)
      }

      private def upsert[T <: Property[_]](p: T) = {
        val ins = p.asInstanceOf[Property[_]]
        val selector: BSONDocument = BSONDocument("_id" -> ins._id)
        collection.update[BSONDocument, Property[_]](selector, ins, upsert = true).transform(ok, failed)
      }

      private def find[V, T <: Property[V]](id: String) = {
        val selector = BSONDocument("_id" -> id)
        val ret = collection.find(selector).one[Property[_]].asInstanceOf[Future[Option[T]]]
        ret.map(_.map(_.value))
      }


      private def ok(wr: WriteResult) = Ok

      private def failed(th: Throwable) = Failure(th)
    }

    new PersistentConfig with StorageHolder with ConfigHolder {
      override def storage: Storage = mongoStorage

      override def cfg: Config = ctx.inject[Config].get
    }
  }

  override def iKey() = classOf[PersistentConfigModule]

}