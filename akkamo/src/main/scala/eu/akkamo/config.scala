package eu.akkamo

import com.typesafe.config.ConfigException

/**
  * Object providing helper functions, data structures and implicit conversions to make the work
  * with Java-based ''Typesafe Config'' in Scala world little more convenient.
  *
  * == Example of use: ==
  *
  * {{{
  *   import eu.akkamo.config._
  *   implicit val config: Config = someConfigInstanceHere
  *
  *   val barValue: String = get[String]("barKey").getOrElse("unknown value")
  * }}}
  */
object config {

  import com.typesafe.config.Config

  import scala.collection.JavaConversions._
  import scala.util.Try

  /**
    * Parses the configuration block, identified by its ''key'', to the Scala map.
    *
    * @param key configuration block key
    * @param cfg configuration to parse
    * @return parsed map
    */
  def blockAsMap(key: String)(implicit cfg: Config): Option[Map[String, Config]] = Try {
    val keys = cfg.getObject(key).keySet()
    keys.map { p => (p, cfg.getConfig(s"$key.$p")) }.toMap.map(p => (p._1, p._2.resolve()))
  }.toOption

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
    */
  def get[T](key: String, cfg: Config)(implicit t: Transformer[T]): Option[T] = {
    getInternal[T](key, cfg, t)
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
    */
  def get[T](key: String)(implicit t: Transformer[T], cfg: Config): Option[T] = {
    getInternal[T](key, cfg, t)
  }

  private def getInternal[T](path: String, cfg: Config, t: Transformer[T]): Option[T] = {
    if (cfg.hasPath(path)) {
      Some(t(cfg, path))
    } else {
      None
    }
  }

  /**
    * ''Typeclass'' config ''transformer'', allowing to extract value of type `T` for the given
    * config key and configuration instance.
    *
    * @tparam T type of the extracted value
    */
  type Transformer[T] = (Config, String) => T

  implicit val cfg2String: Transformer[String] = (cfg: Config, key: String) =>
    cfg.getString(key)

  implicit val cfg2Int: Transformer[Int] = (cfg: Config, key: String) =>
    cfg.getInt(key)

  implicit val cfg2Boolean: Transformer[Boolean] = (cfg: Config, key: String) =>
    cfg.getBoolean(key)

  implicit val cfg2Long: Transformer[Long] = (cfg: Config, key: String) =>
    cfg.getLong(key)

  implicit val cfg2Double: Transformer[Double] = (cfg: Config, key: String) =>
    cfg.getDouble(key)

  implicit val cfg2Config: Transformer[Config] = (cfg: Config, key: String) =>
    cfg.getConfig(key)

  implicit val cfg2IntList: Transformer[List[Int]] = (cfg: Config, key: String) =>
    cfg.getIntList(key).toList.asInstanceOf[List[Int]]

  implicit val cfg2StringList: Transformer[List[String]] = (cfg: Config, key: String) =>
    cfg.getStringList(key).toList

  implicit val cfg2LongList: Transformer[List[Long]] = (cfg: Config, key: String) =>
    cfg.getLongList(key).toList.asInstanceOf[List[Long]]

  implicit val cfg2DoubleList: Transformer[List[Double]] = (cfg: Config, key: String) =>
    cfg.getDoubleList(key).toList.asInstanceOf[List[Double]]

  implicit val cfg2ConfigList: Transformer[List[Config]] = (cfg: Config, key: String) =>
    cfg.getConfigList(key).toList

  implicit val cfg2ConfigMap: Transformer[Map[String, Config]] = (cfg: Config, key: String) => try {
    val keys = cfg.getObject(key).keySet()
    keys.map { p => (p, cfg.getConfig(s"$key.$p")) }.toMap.map(p => (p._1, p._2.resolve()))
  } catch {
    case th: Throwable => throw new ConfigException.Missing(s"Can`t convert config value to map for key:$key ", th)
  }

}