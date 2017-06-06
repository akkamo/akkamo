package eu.akkamo.persistentconfig

import org.mongodb.scala.bson.{BsonArray, BsonValue}
import org.mongodb.scala.bson.collection.mutable.Document


/**
  * interface that allow convert Value to Document and back
  * @tparam T the type
  * @author JuBu
  */
trait VDV[T] {
  /**
    * conversion from a type:T -> Document
    * @param t
    * @return instance of Document
    */
  def apply(t: T): Document


  /**
    *
    * @return function that converts Document -> Option[T]
    */
  def back: (Document) => Option[T]
}

/**
  * interface that allow convert List Value to Document and back to List of Value
  * @tparam T the type
  * @author JuBu
  */
trait ListVDV[T] extends VDV[List[T]] {

  def convert: (BsonValue) => T

  import scala.collection.JavaConverters._

  override val back = (d: Document) => d.get[BsonArray]("obj").map(_.getValues.asScala.toList.map(convert))
}
