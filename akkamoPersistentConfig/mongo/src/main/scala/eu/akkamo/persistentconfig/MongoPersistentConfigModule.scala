package eu.akkamo.persistentconfig

import akka.event.LoggingAdapter
import com.typesafe.config.Config
import eu.akkamo._
import eu.akkamo.mongo.{ReactiveMongoApi, ReactiveMongoModule}
import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.api.commands.WriteResult
import reactivemongo.bson.{BSONDocument, BSONDocumentReader, BSONDocumentWriter, BSONObjectID}

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
  * @author jan.cajthaml <jan.cajthaml@gmail.com>
  */
class MongoPersistentConfigModule extends PersistentConfigModule with Initializable {

  val cfgKey = "persistentConfig"

  override def initialize(ctx: Context) = Try {
    val log = ctx.inject[LoggingAdapterFactory].map(_ (this)).get
    val api = ctx.inject[ReactiveMongoApi](cfgKey).getOrElse(throw InitializableError("Missing ReactiveMongoApi instance!"))
    val cfg = ctx.inject[Config].get
    val register = initialize(log, api, cfg)
    ctx.register[PersistentConfig](register)
  }

  override def dependencies(dependencies: Dependency): Dependency =
    dependencies.&&[ConfigModule].&&[ReactiveMongoModule].&&[LogModule]

  def initialize(log: LoggingAdapter, api: ReactiveMongoApi, config: Config): PersistentConfig = {

    val mongoStorage = new Storage {

      val collection = api.db.collection[BSONCollection]("config")

      implicit val ic = api.driver.system.dispatcher

      private val BooleanPropertyClz = BooleanProperty.getClass.getName
      private val IntPropertyClz = IntProperty.getClass.getName
      private val LongPropertyClz = LongProperty.getClass.getName
      private val StringPropertyClz = StringProperty.getClass.getName
      private val DoublePropertyClz = DoubleProperty.getClass.getName
      private val IntListPropertyClz = IntListProperty.getClass.getName
      private val LongListPropertyClz = LongListProperty.getClass.getName
      private val DoubleListPropertyClz = DoubleListProperty.getClass.getName
      private val StringListPropertyClz = StringListProperty.getClass.getName

      implicit object PropertyReader extends BSONDocumentReader[Property[_]] {

        override def read(bson: BSONDocument) = {
          val id = () => bson.getAs[BSONObjectID]("_id").get.stringify

          bson.getAs[String]("className") match {
            case Some(BooleanPropertyClz) => BooleanProperty(id(), bson.getAs[Boolean]("value").get)
            case Some(IntPropertyClz) => IntProperty(id(), bson.getAs[Int]("value").get)
            case Some(LongPropertyClz) => LongProperty(id(), bson.getAs[Long]("value").get)
            case Some(StringPropertyClz) => StringProperty(id(), bson.getAs[String]("value").get)
            case Some(DoublePropertyClz) => DoubleProperty(id(), bson.getAs[Double]("value").get)
            case Some(IntListPropertyClz) => IntListProperty(id(), bson.getAs[List[Int]]("value").get)
            case Some(LongListPropertyClz) => LongListProperty(id(), bson.getAs[List[Long]]("value").get)
            case Some(DoubleListPropertyClz) => DoubleListProperty(id(), bson.getAs[List[Double]]("value").get)
            case Some(StringListPropertyClz) => StringListProperty(id(), bson.getAs[List[String]]("value").get)
            case x => throw InitializableError(s"Can't read BSON property: ${bson} with className: ${x}")
          }
        }
      }

      implicit object PropertyWriter extends BSONDocumentWriter[Property[_]] {

        override def write(t: Property[_]) = {
          val id = () => BSONObjectID.parse(t._id).get
          t match {
            case x: BooleanProperty => BSONDocument(
              "_id" -> id(),
              "value" -> x.value,
              "className" -> BooleanPropertyClz
            )
            case x: IntProperty => BSONDocument(
              "_id" -> id(),
              "value" -> x.value,
              "className" -> IntPropertyClz
            )
            case x: LongProperty => BSONDocument(
              "_id" -> id(),
              "value" -> x.value,
              "className" -> LongPropertyClz
            )
            case x: DoubleProperty => BSONDocument(
              "_id" -> id(),
              "value" -> x.value,
              "className" -> DoublePropertyClz
            )
            case x: StringProperty => BSONDocument(
              "_id" -> id(),
              "value" -> x.value,
              "className" -> StringPropertyClz
            )
            case x: IntListProperty => BSONDocument(
              "_id" -> id(),
              "value" -> x.value,
              "className" -> IntListPropertyClz
            )
            case x: LongListProperty => BSONDocument(
              "_id" -> id(),
              "value" -> x.value,
              "className" -> LongListPropertyClz
            )
            case x: DoubleListProperty => BSONDocument(
              "_id" -> id(),
              "value" -> x.value,
              "className" -> DoubleListPropertyClz
            )
            case x: StringListProperty => BSONDocument(
              "_id" -> id(),
              "value" -> x.value,
              "className" -> StringListPropertyClz
            )
          }
        }
      }

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

      override def cfg: Config = config
    }
  }

  override def iKey() = classOf[PersistentConfigModule]

}