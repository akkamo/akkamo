package eu.akkamo.persistentconfig

import org.mongodb.scala.bson.{BsonArray, BsonValue}
import org.mongodb.scala.bson.collection.mutable.Document
import scala.collection.JavaConversions._


/**

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
  *
  * @tparam T the type
  * @author JuBu
  */
trait ListVDV[T] extends VDV[List[T]] {

  def convert: (BsonValue) => T

  override val back = (d: Document) => d.get[BsonArray]("v").map(_.getValues.toList.map(convert))
}
