package eu.akkamo.persistentconfig

import org.bson.BsonBoolean
import org.mongodb.scala.bson.collection.mutable.Document
import org.mongodb.scala.bson.{BsonDouble, BsonInt32, BsonInt64, BsonString, BsonValue}

/**
  * @author jubu.
  */
private[persistentconfig] object internalImplicits {

  implicit object StringVDV extends VDV[String] {
    override def apply(t: String): Document = Document("obj" -> t)

    override val back = (d: Document) => d.get[BsonString]("obj").map(_.getValue)
  }

  implicit object StringListVDV extends ListVDV[String] {
    override def apply(t: List[String]): Document = Document("obj" -> t)

    override def convert = (p: BsonValue) => p.asString().getValue
  }

  implicit object IntVDV extends VDV[Int] {
    override def apply(t: Int): Document = Document("obj" -> t)

    override val back = (d: Document) => d.get[BsonInt32]("obj").map(_.getValue)
  }

  implicit object IntListVDV extends ListVDV[Int] {
    override def apply(t: List[Int]): Document = Document("obj" -> t)

    override def convert = (p: BsonValue) => p.asInt32().getValue
  }

  implicit object LongVDV extends VDV[Long] {
    override def apply(t: Long): Document = Document("obj" -> t)

    override val back = (d: Document) => d.get[BsonInt64]("obj").map(_.getValue)
  }

  implicit object LongListVDV extends ListVDV[Long] {
    override def apply(t: List[Long]): Document = Document("obj" -> t)

    override def convert = (p: BsonValue) => p.asInt64().getValue
  }

  implicit object DoubleVDV extends VDV[Double] {
    override def apply(t: Double): Document = Document("obj" -> t)

    override val back = (d: Document) => d.get[BsonDouble]("obj").map(_.getValue)
  }

  implicit object DoubleListVDV extends ListVDV[Double] {
    override def apply(t: List[Double]): Document = Document("obj" -> t)

    override def convert = (p: BsonValue) => p.asDouble().getValue
  }


  implicit object BooleanVDV extends VDV[Boolean] {
    override def apply(t: Boolean): Document = Document("obj" -> t)

    override val back = (d: Document) => d.get[BsonBoolean]("obj").map(_.getValue)
  }

}
