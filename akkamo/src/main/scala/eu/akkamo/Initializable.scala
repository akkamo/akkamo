package eu.akkamo

import com.typesafe.config.{Config, ConfigObject, ConfigValue, ConfigValueType}
import eu.akkamo.m.config._

import scala.reflect.ClassTag

/**
  * Trait indicating that the module extending this requires to perform initialization during Akkamo
  * startup. Initialization is the very first stage of ''Akkamo'' module lifecycle and in this stage,
  * module should check all its required dependencies and/or register its own provided functionality
  * into context.
  *
  * @author jubu
  */
trait Initializable {
  /**
    * Method the module extending this trait must implement, all module initialization logic should
    * be performed here. ''Akkamo'' context is given as a parameter, allowing access to all required
    * dependencies and allows to register module's own functionality. The boolean return value
    * should determine whether the module has been properly initialized or not (e.g. not all
    * dependencies are initialized yet)
    *
    * @param ctx ''Akkamo'' context
    * @return instance of Res that contains (new if modified) instance of [[eu.akkamo.Context]] or
    *         exception packed in ``Try``
    */
  def initialize(ctx: Context): Res[Context]

}

/**
  * Helper methods
  */
object Initializable {

  type Parsed[T] = (Boolean, List[String], T)
  type Builder[T] = ConfigValue => T

  private def typeBuilder[T:Transformer] = new Builder[T] {
    override def apply(c: ConfigValue): T = implicitly[Transformer[T]].apply(c)
  }

  /**
    * Parse list of 'T' from configuration
    * Here is an example:
    * {{{
    *   // let  parse:
    *   case class Foo(x:Int)
    *
    *   // config:
    *   {
    *     foos {
    *       a1 {
    *         x=1
    *       }
    *       a2 {
    *         aliases = ["a3"]
    *         default = true
    *         x = 2
    *       }
    *     }
    *   }
    * }}}
    * parsed result is: List((false, List("a1"), Foo(1)), (true, List("a2", "a3"), Foo(2))) <br/>
    * __Remark:__ Aliases are ordered, and first is from alias  <br/>

    * @param path in configuration
    * @param cfg reference to `Config` instance
    * @param builder of `T`
    * @param ct ClassTag reference
    * @tparam T result type
    * @return list of triplets (Boolean, List[String], T) <=> (default, aliases, instance) encapsulated in Option
    */
  def parseConfig[T](path: String, cfg: Config, builder: Builder[T])(implicit ct: ClassTag[T]):Option[List[Parsed[T]]] = {

    implicit val transformTriplets = new Transformer[Parsed[T]] {
      override def apply(v: ConfigValue): Parsed[T] = {
        if (v.valueType() != ConfigValueType.OBJECT) {
          throw new IllegalArgumentException(s"The value: $v is not `OBJECT`. Can't be parsed to type: ${ct.runtimeClass.getName}")
        }
        val obj = v.asInstanceOf[ConfigObject]
        val default = Option(obj.get("default")).map(implicitly[Transformer[Boolean]].apply(_)).getOrElse(false)
        val aliases = Option(obj.get("aliases")).map(implicitly[Transformer[List[String]]].apply(_)).getOrElse(List.empty)
        (default, aliases, builder(v))
      }
    }
    if (cfg.hasPath(path)) {
      val v = cfg.getValue(path)
      val res = if (v.valueType() == ConfigValueType.OBJECT) {
        (config.as[Map[String, Parsed[T]]](path, cfg).map { case (key, parsed) =>
          parsed.copy(_2 = key :: parsed._2)
        }).toList
      } else throw new IllegalArgumentException(
        s"The value under alias $path is not `OBJECT` (see ConfigValueType)")
      Some(res match {
        case x :: Nil => x.copy(_1 = true) :: Nil // if only one element in List, then is automatically understand as default
        case xs => xs
      })
    } else None
  }

  /**
    * Simplified version of `parseConfig` with default builder instance. Transformer[T] is created on demand.
    *
    * @param path in configuration
    * @param cfg reference to `Config` instance
    * @param ct ClassTag reference
    * @tparam T result type
    * @return list of triplets (Boolean, List[String], T) <=> (default, aliases, instance) encapsulated in Option
    */
  def parseConfig[T:Transformer](path: String, cfg: Config)(implicit ct: ClassTag[T]):
  Option[List[Parsed[T]]] = parseConfig(path, cfg, typeBuilder[T])


  /**
    * structural validation of parsed values
    *
    * @param parsed result of parsed config values
    * @tparam T type of parsed
    * @return triplet: (Boolean, Boolean, Boolean) meaning:
    *         (contains at least one value, has one default element, has unique aliases)
    */
  def validate[T](parsed: List[Parsed[T]]): (Boolean, Boolean, Boolean) = {
    val notEmpty = !parsed.isEmpty
    val oneDefault = if (notEmpty) {
      parsed.foldLeft(0) { case (res, (default, _, _)) => res + (if (default) 1 else 0) } == 1
    } else false
    val uniqueAliases = if (notEmpty) {
      val all = parsed.foldLeft(0) { case (res, (_, aliases, _)) => res + aliases.size }
      val union = parsed.flatMap(_._2).toSet.size
      all == union
    } else true
    (notEmpty, oneDefault, uniqueAliases)
  }


  /**
    * Call `validate` and throws [[InitializableError]] if validation fails.
    *
    * @param key
    * @param parsed
    * @tparam T
    * @throws InitializableError
    * @return list of triplets (Boolean, List[String], T) <=> (default, aliases, instance)
    */
  @throws[InitializableError]
  def defaultReport[T](key: String, parsed: List[Parsed[T]]): List[Parsed[T]] = {
    val (notEmpty, oneDefault, uniqueAliases) = validate(parsed)
    if (!notEmpty) throw InitializableError(s"Empty configuration for: $key detected.")
    if (!oneDefault) throw InitializableError(s"Multiple `default` values for: $key detected.")
    if (!uniqueAliases) throw InitializableError(s"Ambigious aliases for: $key detected.")

    parsed
  }
}

/**
  * Recommended Error to be thrown inside [[eu.akkamo.Initializable#initialize]] method if serious
  * unrecoverable problem occurs.
  *
  * @param message error message
  * @param cause   optional value of cause
  */
case class InitializableError(message: String, cause: Throwable = null) extends AkkamoError(message, cause)
