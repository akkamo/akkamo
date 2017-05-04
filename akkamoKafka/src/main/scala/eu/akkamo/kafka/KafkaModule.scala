package eu.akkamo.kafka

import java.io.{File, FileInputStream, InputStream}
import java.util.Properties
import java.util.concurrent.TimeUnit

import com.typesafe.config.Config
import eu.akkamo.{InitializableError, _}
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer

import scala.collection.immutable.Iterable
import scala.reflect.ClassTag
import scala.util.{Failure, Success, Try}

/**
  * = Configuration example =
  * {{{
  *   akkamo.kafka = {
  *     // complete configuration with several name aliases
  *     name1 = {
  *       aliases = ["alias1", "alias2"]
  *       properties = "name1.properties" // path to property file
  *       producer = true
  *       consumer = true
  *     },
  *     // configuration registered as default (only one instance is allowed)
  *     name2 = {
  *       default = true
  *       properties = "name2.properties"
  *       consumer = true
  *     }
  *   }
  * }}}
  *
  * @author ladislavskokan
  * @author JuBu
  *
  */
class KafkaModule extends Module with Initializable with Disposable with Publisher {

  import config.implicits._

  private case class Def(producer: Boolean, consumer: Boolean, properties: Properties,
                         isDefault: Boolean, aliases: List[String])

  type KC = KafkaConsumer[AnyRef, AnyRef]
  type KP = KafkaProducer[AnyRef, AnyRef]

  private val key = "akkamo.kafka"

  private val Producer = "producer"
  private val Consumer = "consumer"
  private val Properties = "properties"
  private val Default = "default"
  private val Aliases = "aliases"

  override def initialize(ctx: Context) = Try {
    implicit val log = ctx.inject[LoggingAdapterFactory].map(_ (this)).get
    implicit val c = ctx.inject[Config].get

    val defs = normalize(config.getOptAs[Map[String, Config]](key).map(_.map { case (key, cfg) => buildDef(key, cfg) }).getOrElse {
      val properties = loadProperties("kafka-default.properties")
      Def(producer = true, consumer = true, properties, isDefault = true, List.empty) :: Nil
    })

    defs.foldLeft(ctx) { (ctx, p) =>
      val ctx1 = if (p.consumer) {
        val k = new KC(p.properties)
        process(k, p, ctx)
      } else {
        ctx
      }
      if (p.producer) {
        val k = new KP(p.properties)
        process(k, p, ctx1)
      } else {
        ctx1
      }
    }
  }

  private def normalize(defs: Iterable[Def]) = {
    // has only one default ?
    val dc = defs.foldLeft(0)((l, r) => if (r.isDefault) 1 else 0)
    if (dc > 1) {
      throw InitializableError(s"Ambiguous default instances in Kafka configurations")
    }
    if (dc == 0 && defs.size > 1) {
      throw InitializableError(s"Missing default instances in Kafka configurations")
    }
    // transform to have isDefault = true
    if (dc == 0 && defs.size == 1) {
      defs.map(_.copy(isDefault = true))
    } else {
      defs
    }
  }

  private def process[K <: AnyRef](k: K, p: Def, ctx: Context)(implicit ct: ClassTag[K]) = {
    val ctx1 = if (p.isDefault) {
      ctx.register[K](k)
    } else {
      ctx
    }
    p.aliases.foldLeft(ctx1) { (ctx, alias) =>
      ctx.register[K](k, Some(alias))
    }
  }


  private def loadProperties(path: String): Properties = {
    def loadFromClassPath: Option[InputStream] =
      Option.apply(this.getClass.getResourceAsStream(s"/$path"))

    def loadFromDir(dir: String): Option[InputStream] =
      Try(new FileInputStream(new File(dir, path))).toOption

    def loadFromAbsolutePath: Option[InputStream] = loadFromDir(null)

    val streamOpt: Option[InputStream] = loadFromAbsolutePath
      .orElse(loadFromClassPath)
      .orElse(loadFromDir(System.getProperty("user.dir")))
      .orElse(loadFromDir(System.getProperty("user.home")))

    streamOpt match {
      case None => throw InitializableError(s"Missing properties file: $path")
      case Some(stream) =>
        val properties: Properties = new Properties()
        try {
          properties.load(stream)
        } catch {
          case th: Throwable => throw InitializableError(s"Can't read properties from file: $path", th)
        } finally {
          if (stream != null) try {
            stream.close()
          } catch {
            case th: Throwable => th.printStackTrace()
          }
        }
        properties
    }
  }

  override def dependencies(dependencies: Dependency): Dependency =
    dependencies.&&[LoggingAdapterFactory].&&[Config]

  override def publish(): Set[Class[_]] = Set(classOf[KC], classOf[KP])

  private def buildDef(key: String, cfg: Config)(implicit log: LoggingAdapter) = {
    val propertiesFileName = config.get[String](Properties, cfg)
      .getOrElse(throw InitializableError(s"Missing properties file name under definition key:$key"))

    Def(
      config.get[Boolean](Producer, cfg).getOrElse(false),
      config.get[Boolean](Consumer, cfg).getOrElse(false),
      loadProperties(propertiesFileName),
      config.get[Boolean](Default, cfg).getOrElse(false),
      key :: config.get[List[String]](Aliases, cfg).getOrElse(List.empty)
    )
  }


  override def dispose(ctx: Context) = {

    // error handler
    val eh = (ex: DisposableError, v: Try[Unit]) => v match {
      case Failure(th) => ex.addSuppressed(th); ex
      case _ => ex
    }

    val err = DisposableError("Can't dispose some kafka instances")

    ctx.registered[KP].map { p =>
      Try {
        // close operation is blocking
        // TODO - put timeout to config
        p._1.close(60, TimeUnit.SECONDS)
      }
    }.foldLeft(err)(eh)

    ctx.registered[KC].map { p =>
      Try {
        p._1.unsubscribe()
      }
    }.foldLeft(err)(eh)

    // return error if not empty suppressed
    if (err.getSuppressed.length == 0) Success(()) else Failure.apply[Unit](err)
  }
}