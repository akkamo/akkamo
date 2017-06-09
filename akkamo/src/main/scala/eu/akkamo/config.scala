package eu.akkamo

import com.typesafe.config.Config
import eu.akkamo.m.config._


/**
  * @author jubu.
  */
package object config {


  /**
    * This method serves as convenient shorthand of native ''Typesafe Config'' `Config.getXX`
    * methods, as it provides extensible ''typeclass''-based parsing of desired value type and
    * returns result as value.
    *
    * @param path config path
    * @param t    configuration value transformer ''typeclass''
    * @param cfg  configuration instance
    * @tparam T type of requested value
    * @throws ConfigError if can't find value or if conversion form config is no feasible
    * @return value if found exists else throws ConfigError exception
    */
  @inline
  @throws[ConfigError]
  def as[T](path: String)(implicit t: Transformer[T], cfg: Config): T =
  getInternal[T](path, cfg)(t).getOrElse(throw eu.akkamo.config.ConfigError(s"Can't find registered value under path: ${path}"))


  /**
    * This method serves as convenient shorthand of native ''Typesafe Config'' `Config.getXX`
    * methods, as it provides extensible ''typeclass''-based parsing of desired value type and
    * returns result as value.
    *
    * @param path config path
    * @param cfg configuration instance
    * @param t   configuration value transformer ''typeclass''
    * @tparam T type of requested value
    * @throws ConfigError if can't find value or if conversion form config is no feasible
    * @return value (if found)
    */
  @inline
  @throws[ConfigError]
  def as[T](path: String, cfg: Config)(implicit t: Transformer[T]): T = as(path)(t, cfg)

  /**
    * This method serves as convenient shorthand of native ''Typesafe Config'' `Config.getXX`
    * methods, as it provides extensible ''typeclass''-based parsing of desired value type and
    * returns result as optional value.
    *
    * @param path config path
    * @param t   configuration value transformer ''typeclass''
    * @param cfg configuration instance (implicitly provided)
    * @tparam T type of requested value
    * @throws ConfigError if conversion form config is no feasible
    * @return value (if found)
    */
  @inline
  @throws[ConfigError]
  def asOpt[T](path: String)(implicit t: Transformer[T], cfg: Config): Option[T] = getInternal[T](path, cfg)(t)

  /**
    * This method serves as convenient shorthand of native ''Typesafe Config'' `Config.getXX`
    * methods, as it provides extensible ''typeclass''-based parsing of desired value type and
    * returns result as optional value.
    *
    * @param path config path
    * @param t   configuration value transformer ''typeclass''
    * @param cfg configuration instance (implicitly provided)
    * @tparam T type of requested value
    * @throws ConfigError if conversion form config is no feasible
    * @return value (if found)
    */
  @inline
  @throws[ConfigError]
  def asOpt[T](path: String, cfg: Config)(implicit t: Transformer[T]): Option[T] = getInternal[T](path, cfg)(t)

  private def getInternal[T](path: String, cfg: Config)(implicit t: Transformer[T]): Option[T] = try {
    if (cfg.hasPath(path)) {
      Some(t(path, cfg))
    } else {
      None
    }
  } catch {
    case th: Throwable => throw ConfigError(th.getMessage, th)
  }
}
