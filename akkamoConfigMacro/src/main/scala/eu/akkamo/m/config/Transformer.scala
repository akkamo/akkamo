package eu.akkamo.m.config

import com.typesafe.config.{Config, ConfigObject, ConfigValue}

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
    if(v == null) throw new NullPointerException(s"Object under key: $key doesn't exists")
    else try {
      this(v)
    } catch {
      case th:Throwable => throw new Exception(s"Can't parse value under key: $key. ${th.getMessage}", th)
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
        case th:Throwable => throw new Exception(s"Can't parse value under path: $path. ${th.getMessage}", th)
      }
    } else throw new NullPointerException(s"Value under path: $path doesn't exists")
  }
}
