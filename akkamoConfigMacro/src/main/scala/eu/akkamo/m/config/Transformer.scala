package eu.akkamo.m.config

import com.typesafe.config.{Config, ConfigList, ConfigObject, ConfigValue}

import scala.collection.immutable.ListMap

/**
  * ''Typeclass'' config ''transformer'', allowing to extract value of type `T` for the given configuration value.
  *
  * @tparam T type of the extracted value
  */
trait Transformer[T] {

  /**
    *
    * @param v instance of ``com.typesafe.config.ConfigValue`` or `null`
    * @return instance created from a `ConfigValue` or throws Exception if value can't be parsed
    */
  // TODO add throws in future
  //@throws java.lang.Throwable in value can't be transformed, or NullPointerException 'obj' is null
  @throws[Throwable]
  def apply(v: ConfigValue): T


  /**
    *
    * @param key
    * @param obj
    * @return
    */
  def apply(key:String, obj:ConfigObject):T =  {
    val v = obj.get(key)
    if(v == null) throw new NullPointerException(s"""Object under key: "$key" doesn't exists""")
    else try {
      this(v)
    } catch {
      case th:Throwable => throw new Exception(s"""Can't parse value under key: "$key". ${th.getMessage}""", th)
    }
  }

  /**
    *
    * @param path
    * @param cfg
    * @return
    */
  def apply(path:String, cfg:Config):T  = {
    if (cfg.hasPath(path)) {
      try {
        this(cfg.getValue(path))
      } catch {
        case th:Throwable => throw new Exception(s"""Can't parse value under path: "$path". ${th.getMessage}""", th)
      }
    } else throw new NullPointerException(s"""Value under path: "$path" doesn't exists""")
  }
}

object Transformer {

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
      uw match {
        case number: Number => BigDecimal.decimal(number.doubleValue())
        case string: String => BigDecimal(string)
        case _ => throw new IllegalArgumentException(s"Can't parse value: $v as BigDecimal")
      }
    }
  }

  implicit object CV2BigInt extends Transformer[BigInt] {
    override def apply(v: ConfigValue): BigInt = {
      val uw = v.unwrapped()
      uw match {
        case number: Number => BigInt(number.longValue())
        case string: String => BigInt(string)
        case _ => throw new IllegalArgumentException(s"Can't parse value: $v as BigInt")
      }
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
    override def apply(key: String, obj: ConfigObject): Option[T] = Option(obj.get(key)).flatMap(this(_))
    override def apply(path: String, cfg: Config): Option[T] =
      if (cfg.hasPath(path)) { this(cfg.getValue(path)) } else None
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
    def c(t: Transformer[T], it: java.util.Iterator[String], cfg: ConfigObject,
                  res: Map[String, T] = ListMap.empty[String, T]): Map[String, T] =
      if (it.hasNext) {
        val key = it.next()
        val value = t(cfg.get(key))
        val newMap = res + (key -> value)
        c(t, it, cfg, newMap)
      } else res
  }


  implicit def materializeTransformer[T]: Transformer[T] = macro TransformerGenerator.buildTransformer[T]
}