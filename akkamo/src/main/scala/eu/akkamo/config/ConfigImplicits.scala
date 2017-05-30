package eu.akkamo.config

import com.typesafe.config.{Config, ConfigList, ConfigObject, ConfigValue}
import eu.akkamo.m.config.Transformer

import scala.collection.immutable.ListMap

/**
* @author jubu.
*/
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

  implicit def cv2OPtion[T: Transformer] = new Transformer[Option[T]] {
    override def apply(v: ConfigValue): Option[T] = {
      val c = implicitly[Transformer[T]]
      if(v != null) {
        Some(c(v))
      } else None
    }
  }

  implicit def cv2List[T: Transformer] = new Transformer[List[T]] {
    import scala.collection.JavaConverters._
    override def apply(v: ConfigValue): List[T] = {
      val c = implicitly[Transformer[T]]
      v.asInstanceOf[ConfigList].asScala.map(c(_)).toList
    }
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
