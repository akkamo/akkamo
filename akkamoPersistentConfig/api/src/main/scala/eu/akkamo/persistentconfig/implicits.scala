package eu.akkamo.persistentconfig

import com.typesafe.config.Config
import eu.akkamo.m.config.Transformer

import scala.concurrent.Future


/**
	* @author jubu
	*/
object implicits {

	import eu.akkamo.config._
	import eu.akkamo.config.implicits._

	type FromStorage[T] = (String)=>Future[Option[T]]

	object Reader {
		import scala.concurrent.ExecutionContext.Implicits.global

		def apply[T](key: String, f:FromStorage[T], cfg:Config)(implicit t:Transformer[T]): Future[T] = {
			f(key).map(_.getOrElse(getFromConfig[T](key, cfg)))
		}
		def getFromConfig[T](key:String, cfg:Config)(implicit t:Transformer[T]) =
			asOpt[T](key, cfg).getOrElse(throw PersistentConfigException(s"persistent property under alias: $key doesn't exist"))
	}

	implicit val stringReader:PersistentConfig#Reader[String] =
		(storage:Storage, cfg:Config, key:String) =>	Reader(key, storage.getString, cfg)

	implicit val intReader:PersistentConfig#Reader[Int] =
		(storage:Storage, cfg:Config, key:String) => Reader(key, storage.getInt, cfg)

	implicit val booleanReader:PersistentConfig#Reader[Boolean] =
		(storage:Storage, cfg:Config, key:String) => Reader(key, storage.getBoolean, cfg)

	implicit val longReader:PersistentConfig#Reader[Long] =
		(storage:Storage, cfg:Config,  key:String) => Reader(key,storage.getLong, cfg)

	implicit val doubleReader:PersistentConfig#Reader[Double] =
		(storage:Storage, cfg:Config, key:String) => Reader(key,storage.getDouble, cfg)

	implicit val stringListReader:PersistentConfig#Reader[List[String]] =
		(storage:Storage, cfg:Config, key:String) => Reader(key, storage.getStringList, cfg)

	implicit val intListReader:PersistentConfig#Reader[List[Int]] =
		(storage:Storage, cfg:Config, key:String) => Reader(key,storage.getIntList, cfg)

	implicit val longListReader:PersistentConfig#Reader[List[Long]] =
		(storage:Storage, cfg:Config, key:String) => Reader(key,storage.getLongList, cfg)

	implicit val doubleListReader:PersistentConfig#Reader[List[Double]] =
		(storage:Storage, cfg:Config,  key:String) => Reader(key,storage.getDoubleList, cfg)

	implicit val stringWriter:PersistentConfig#Writer[String] = (storage:Storage, key:String, value:String) => storage.storeString(key, value)

	implicit val intWriter:PersistentConfig#Writer[Int] = (storage:Storage, key:String, value:Int) => storage.storeInt(key, value)

	implicit val booleanWriter:PersistentConfig#Writer[Boolean] = (storage:Storage, key:String, value:Boolean) => storage.storeBoolean(key, value)

	implicit val longWriter:PersistentConfig#Writer[Long] = (storage:Storage, key:String, value:Long) => storage.storeLong(key, value)

	implicit val doubleWriter:PersistentConfig#Writer[Double] = (storage:Storage, key:String, value:Double) => storage.storeDouble(key, value)

	implicit val stringListWriter:PersistentConfig#Writer[List[String]] = (storage:Storage, key:String, value:List[String]) => storage.storeStringList(key, value)

	implicit val intListWriter:PersistentConfig#Writer[List[Int]] = (storage:Storage, key:String, value:List[Int]) => storage.storeIntList(key, value)

	implicit val longListWriter:PersistentConfig#Writer[List[Long]] = (storage:Storage, key:String, value:List[Long]) => storage.storeLongList(key, value)

	implicit val doubleListWriter:PersistentConfig#Writer[List[Double]] = (storage:Storage, key:String, value:List[Double]) => storage.storeDoubleList(key, value)
}
