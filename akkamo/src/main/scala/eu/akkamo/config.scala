package eu.akkamo

import com.typesafe.config.Config
import eu.akkamo.m.config.{Transformer, TransformerGenerator}


/**
  * @author jubu.
  */
package object config {
  import implicits._

  /**
    * Parses the configuration block, identified by its ''key'', to the Scala map.
    *
    * @param key configuration block key
    * @param cfg configuration to parse
    * @return parsed map
    * @deprecated use `get[Map[String, Config]]`
    */
  @deprecated(message = "Use asOpt[Map[String, Config]]", since="1.1.0")
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
    * @throws ConfigError if conversion form config is no feasible
    * @return value (if found)
    * @deprecated use as[T]
    */
  @deprecated(message = "Use asOpt[T] or as[T]", since="1.1.0")
  @throws[ConfigError]
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
    * @throws ConfigError if conversion form config is no feasible
    * @return value (if found)
    * @deprecated use as[T]
    */
  @deprecated(message = "Use asOpt[T] or as[T]", since="1.1.0")
  @throws[ConfigError]
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
    * @throws ConfigError if can't find value or if conversion form config is no feasible
    * @return value if found exists else throws ConfigError exception
    */
  @inline
  @throws[ConfigError]
  def as[T](key: String)(implicit t: Transformer[T], cfg: Config): T =
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
    * @throws ConfigError if can't find value or if conversion form config is no feasible
    * @return value (if found)
    */
  @inline
  @throws[ConfigError]
  def as[T](key: String, cfg: Config)(implicit t: Transformer[T]): T = as(key)(t, cfg)

  /**
    * This method serves as convenient shorthand of native ''Typesafe Config'' `Config.getXX`
    * methods, as it provides extensible ''typeclass''-based parsing of desired value type and
    * returns result as optional value.
    *
    * @param key config key
    * @param t   configuration value transformer ''typeclass''
    * @param cfg configuration instance (implicitly provided)
    * @tparam T type of requested value
    * @throws ConfigError if conversion form config is no feasible
    * @return value (if found)
    */
  @inline
  @throws[ConfigError]
  def asOpt[T](key: String)(implicit t: Transformer[T], cfg: Config): Option[T] = getInternal[T](key, cfg)(t)

  /**
    * This method serves as convenient shorthand of native ''Typesafe Config'' `Config.getXX`
    * methods, as it provides extensible ''typeclass''-based parsing of desired value type and
    * returns result as optional value.
    *
    * @param key config key
    * @param t   configuration value transformer ''typeclass''
    * @param cfg configuration instance (implicitly provided)
    * @tparam T type of requested value
    * @throws ConfigError if conversion form config is no feasible
    * @return value (if found)
    */
  @inline
  @throws[ConfigError]
  def asOpt[T](key: String, cfg: Config)(implicit t: Transformer[T]): Option[T] = getInternal[T](key, cfg)(t)

  private def getInternal[T](path: String, cfg: Config)(implicit t: Transformer[T]): Option[T] = try {
    if (cfg.hasPath(path)) {
      Some(t(cfg.getValue(path)))
    } else {
      None
    }
  } catch {
    case th:NullPointerException => throw ConfigError(s"The value, or value parameter under path: $path doesn't exists", th)
    case th:ConfigError => throw th
    case th:Throwable => throw ConfigError(s"Can't parse value under path: $path", th)
  }

  import language.experimental.macros

  def generateTransformer[T]:Transformer[T] = macro TransformerGenerator.buildTransformer[T]
}
