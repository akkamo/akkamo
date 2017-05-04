package eu.akkamo

import com.typesafe.config.{Config, ConfigList, ConfigObject, ConfigValue}

import scala.collection.immutable.ListMap

//import scala.collection.immutable.ListMap


package config  {

  /**
    * ''Typeclass'' config ''transformer'', allowing to extract value of type `T` for the given
    * config key and configuration instance.
    *
    * @tparam T type of the extracted value
    */
  trait Transformer[T] {
    def apply(v:ConfigValue):T
  }


  /**
    * Error thrown when value under given key does not exists
    *
    * @param message detail message
    * @param cause   optional error cause
    */
  case class ConfigError(message: String, cause: Throwable = null) extends AkkamoError(message, cause)

  trait ConfigImplicits {

    implicit object CV2String extends Transformer[String] {
      override def apply(v: ConfigValue):String  = v.unwrapped().asInstanceOf[String]
    }

    implicit object CV2Cfg extends Transformer[Config] {
      override def apply(v: ConfigValue): Config = v.asInstanceOf[ConfigObject].toConfig
    }

    implicit object CV2Long extends Transformer[Long] {
      override def apply(v: ConfigValue): Long = v.unwrapped().asInstanceOf[Number].longValue()
    }

    implicit object CV2Int extends Transformer[Int] {
      override def apply(v: ConfigValue): Int = v.unwrapped().asInstanceOf[Number].intValue()
    }

    implicit object CV2Double extends Transformer[Double] {
      override def apply(v: ConfigValue): Double = v.unwrapped().asInstanceOf[Number].doubleValue()
    }

    implicit object CV2BigDecimal extends Transformer[BigDecimal] {
      override def apply(v: ConfigValue): BigDecimal = {
        val uw = v.unwrapped()
        if(uw.isInstanceOf[Number])  BigDecimal.decimal(uw.asInstanceOf[Number].doubleValue())
        else if (uw.isInstanceOf[String]) BigDecimal(uw.asInstanceOf[String])
        else throw ConfigError(s"Can't parse value: $v as BigDecimal")
      }
    }


    implicit object CVBoolean extends Transformer[Boolean] {
      override def apply(v: ConfigValue): Boolean = v.unwrapped().asInstanceOf[Boolean]
    }

    implicit def cv2List[T: Transformer] = new Transformer[List[T]] {
      override def apply(v: ConfigValue): List[T] = c(implicitly[Transformer[T]], v.asInstanceOf[ConfigList].iterator())

      @inline
      private def c(t: Transformer[T], it: java.util.Iterator[ConfigValue], res: List[T] = List.empty): List[T] =
        if (it.hasNext) {
          c(t, it, t(it.next()) +: res)
        } else res
    }

    implicit def cv2Map[T: Transformer] = new Transformer[Map[String, T]] {
      override def apply(v: ConfigValue): Map[String, T] =
        c(implicitly[Transformer[T]], v.asInstanceOf[ConfigObject].keySet().iterator(), v.asInstanceOf[ConfigObject])

      @inline
      private def c(t: Transformer[T], it: java.util.Iterator[String], cfg: ConfigObject,
                    res: Map[String, T] = ListMap.empty[String, T]): Map[String, T] =
        if (it.hasNext) {
          val key = it.next()
          val value = t(cfg.get(key))
          val newMap = res + (key -> value)
          c(t, it, cfg, newMap)
        } else res
    }
  }

  object implicits extends ConfigImplicits
}

/**
  * Object providing helper functions, data structures and implicit conversions to make the work
  * with Java-based ''Typesafe Config'' in Scala world little more convenient.
  *
  * == Example of use: ==
  *
  * {{{
  *   import eu.akkamo.config
  *   import config.implicits_
  *   implicit val cfg: Config = someConfigInstanceHere
  *
  *   val barValue: String = config.get[String]("barKey").getOrElse("unknown value")
  * }}}
  */
package object config {
  import config.implicits._

  /**
    * Parses the configuration block, identified by its ''key'', to the Scala map.
    *
    * @param key configuration block key
    * @param cfg configuration to parse
    * @return parsed map
    * @deprecated use `get[Map[String, Config]]`
    */

  def blockAsMap(key: String)(implicit cfg: Config): Option[Map[String, Config]] = {
    getInternal[Map[String, Config]](key, cfg)
  }


  /**
    * This method serves as convenient shorthand of native ''Typesafe Config'' `Config.getXX`
    * methods, as it provides extensible ''typeclass''-based parsing of desired value type and
    * returns result as optional value.
    *
    * @param key config key
    * @param cfg configuration instance
    * @param t   configuration value transformer ''typeclass''
    * @tparam T type of requested value
    * @return value (if found)
    * @deprecated use getAs or getOptAs
    */
  def get[T](key: String, cfg: Config)(implicit t: Transformer[T]): Option[T] = {
    getInternal[T](key, cfg)(t)
  }


  /**
    * This method serves as convenient shorthand of native ''Typesafe Config'' `Config.getXX`
    * methods, as it provides extensible ''typeclass''-based parsing of desired value type and
    * returns result as optional value.
    *
    * @param key config key
    * @param t   configuration value transformer ''typeclass''
    * @param cfg configuration instance (implicitly provided)
    * @tparam T type of requested value
    * @return value (if found)
    * @deprecated use getAs or getOptAs
    */
  def get[T](key: String)(implicit t: Transformer[T], cfg: Config): Option[T] = {
    getInternal[T](key, cfg)(t)
  }


  /**
    * This method serves as convenient shorthand of native ''Typesafe Config'' `Config.getXX`
    * methods, as it provides extensible ''typeclass''-based parsing of desired value type and
    * returns result as value.
    *
    * @param key config key
    * @param t   configuration value transformer ''typeclass''
    * @param cfg configuration instance
    * @tparam T type of requested value
    * @return value (if found)
    */
  def getAs[T](key: String)(implicit t: Transformer[T], cfg: Config): T =
    getInternal[T](key, cfg)(t).getOrElse(throw ConfigError(s"Can't find registered value under key: ${key}"))


  /**
    * This method serves as convenient shorthand of native ''Typesafe Config'' `Config.getXX`
    * methods, as it provides extensible ''typeclass''-based parsing of desired value type and
    * returns result as value.
    *
    * @param key config key
    * @param cfg configuration instance
    * @param t   configuration value transformer ''typeclass''
    * @tparam T type of requested value
    * @return value (if found)
    */
  @inline
  def getAs[T](key: String, cfg: Config)(implicit t: Transformer[T]): T = getAs(key)(t, cfg)

  /**
    * This method serves as convenient shorthand of native ''Typesafe Config'' `Config.getXX`
    * methods, as it provides extensible ''typeclass''-based parsing of desired value type and
    * returns result as optional value.
    *
    * @param key config key
    * @param t   configuration value transformer ''typeclass''
    * @param cfg configuration instance (implicitly provided)
    * @tparam T type of requested value
    * @return value (if found)
    */
  @inline
  def getOptAs[T](key: String)(implicit t: Transformer[T], cfg: Config): Option[T] = getInternal[T](key, cfg)(t)

  /**
    * This method serves as convenient shorthand of native ''Typesafe Config'' `Config.getXX`
    * methods, as it provides extensible ''typeclass''-based parsing of desired value type and
    * returns result as optional value.
    *
    * @param key config key
    * @param t   configuration value transformer ''typeclass''
    * @param cfg configuration instance (implicitly provided)
    * @tparam T type of requested value
    * @return value (if found)
    */
  @inline
  def getOptAs[T](key: String, cfg: Config)(implicit t: Transformer[T]): Option[T] = getOptAs(key)(t, cfg)


  private def getInternal[T](path: String, cfg: Config)(implicit t: Transformer[T]): Option[T] = try {
    if (cfg.hasPath(path)) {
      Some(t(cfg.getValue(path)))
    } else {
      None
    }
  } catch {
    case th:Throwable => throw ConfigError(s"Can't parse value under path: $path", th)
  }
}
