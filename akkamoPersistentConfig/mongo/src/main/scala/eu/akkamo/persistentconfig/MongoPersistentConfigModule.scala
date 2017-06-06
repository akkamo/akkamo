package eu.akkamo.persistentconfig

import com.typesafe.config.Config
import eu.akkamo._
import eu.akkamo.mongo.MongoApi
import org.mongodb.scala.bson.BsonString
import org.mongodb.scala.bson.collection.mutable.Document
import org.mongodb.scala.model.{Filters, UpdateOptions}

import scala.concurrent.Future

/**
  * @author jubu
  */
class MongoPersistentConfigModule extends PersistentConfigModule with Initializable {

  val cfgKey = "akkamo.mongoPersistentConfig"

  val ColName = "persistentConfig"

  import scala.concurrent.ExecutionContext.Implicits.global

  override def dependencies(dependencies: Dependency): Dependency =
    dependencies.&&[Config].&&[MongoApi].&&[LoggingAdapterFactory]

  def getCollection(ctx: Context) = Future {
    ctx.get[MongoApi](Some(cfgKey.replace(".", "_")))
  }.flatMap { api =>
    val db = api.db
    db.listCollectionNames().toFuture().flatMap { collections =>
      if (collections.contains(ColName)) {
        // if collection exist just return it
        Future.successful(db.getCollection[Document](ColName))
      } else {
        // create and get collection
        db.createCollection(ColName).toFuture().map { _ =>
          db.getCollection[Document](ColName)
        }
      }
    }
  }


  override def initialize(ctx: Context) = getCollection(ctx).map { collection =>

    val mongoStorage = new Storage {

      import internalImplicits._

      override def getString(key: String): Future[Option[String]] = find[String](key)

      override def getIntList(key: String): Future[Option[List[Int]]] = find[List[Int]](key)

      override def storeStringList(key: String, value: List[String]): Future[Unit] = upsert(key, value)

      override def storeLongList(key: String, value: List[Long]): Future[Unit] = upsert(key, value)

      override def storeBoolean(key: String, value: Boolean): Future[Unit] = upsert(key, value)

      override def getLongList(key: String): Future[Option[List[Long]]] = find[List[Long]](key)

      override def getDouble(key: String): Future[Option[Double]] = find[Double](key)

      override def storeString(key: String, value: String): Future[Unit] = upsert(key, value)

      override def getDoubleList(key: String): Future[Option[List[Double]]] = find[List[Double]](key)

      override def getLong(key: String): Future[Option[Long]] = find[Long](key)

      override def storeDouble(key: String, value: Double): Future[Unit] = upsert(key, value)

      override def storeInt(key: String, value: Int): Future[Unit] = upsert(key, value)

      override def storeDoubleList(key: String, value: List[Double]): Future[Unit] = upsert(key, value)

      override def storeLong(key: String, value: Long): Future[Unit] = upsert(key, value)

      override def getBoolean(key: String): Future[Option[Boolean]] = find[Boolean](key)

      override def storeIntList(key: String, value: List[Int]): Future[Unit] = upsert(key, value)

      override def getStringList(key: String): Future[Option[List[String]]] = find[List[String]](key)

      override def getInt(key: String): Future[Option[Int]] = find[Int](key)


      override def remove(id: String): Future[Unit] = {
        val selector = Filters.eq("_id", BsonString(id))
        collection.deleteOne(selector).toFuture.map(p=>())
      }

      private def upsert[V: VDV](id: String, v: V) = {
        val c = implicitly[VDV[V]]
        val selector = Document("_id" -> id)
        collection.updateOne(selector, Document("$set"-> (selector ++ c(v))), new UpdateOptions().upsert(true)).toFuture().map(p=>())
      }

      private def find[V: VDV](id: String) = {
        val c = implicitly[VDV[V]]
        val selector = Filters.eq("_id", BsonString(id))
        val ret = collection.find[Document](selector)
        ret.toFuture.map(_.headOption.flatMap(c.back))
      }
    }

    val register = new PersistentConfig with StorageHolder with ConfigHolder {
      override def storage: Storage = mongoStorage

      override def cfg = ctx.get[Config]
    }
    ctx.register[PersistentConfig](register)
  }

  override def iKey() = classOf[PersistentConfigModule]

}