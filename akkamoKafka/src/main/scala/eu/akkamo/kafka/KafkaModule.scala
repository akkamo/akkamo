package eu.akkamo.kafka

import java.util.Properties

import akka.event.LoggingAdapter
import com.typesafe.config.Config
import eu.akkamo.{InitializableError, _}
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer

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
class KafkaModule extends Module with Initializable with Disposable {

  import config._

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


  @scala.throws[InitializableError]("If initialization can't be finished")
  override def initialize(ctx: Context) = Try {
    implicit val log: LoggingAdapter = ctx.inject[LoggingAdapterFactory].map(_ (this)).get
    implicit val c = ctx.inject[Config].get
    val defs = blockAsMap(key).map(_.map { case (key, cfg) => buildDef(key, cfg) }).getOrElse {
      val properties = loadProperties("kafka-default.properties")
      Def(producer = true, consumer = true, properties, isDefault = true, List.empty) :: Nil
    }
    // has only one default ?
    val dc = defs.foldLeft(0)((l, r) => if (r.isDefault) 1 else 0)
    if (dc > 1) {
      throw InitializableError(s"Ambiguous default instances in Kafka configurations")
    }

    def process[K <: AnyRef](k: K, p: Def, ctx: Context)(implicit ct: ClassTag[K]) = {
      val ctx1 = if (p.isDefault) {
        ctx.register[K](k)
      } else {
        ctx
      }
      p.aliases.foldLeft(ctx1) { (ctx, alias) =>
        ctx.register[K](k, Some(alias))
      }
    }

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

  override def dependencies(dependencies: Dependency): Dependency =
    dependencies.&&[LogModule].&&[ConfigModule]

  private def loadProperties(name: String)(implicit log: LoggingAdapter) = {
    val res = Thread.currentThread.getContextClassLoader.getResourceAsStream(name)
    if (res == null) {
      throw InitializableError(s"Missing properties file: $name")
    }
    val properties = new Properties()
    try {
      properties.load(res)
    } catch {
      case th: Throwable => throw InitializableError(s"Can't read properties from file: $name", th)
    } finally {
      if (res != null) try {
        res.close()
      } catch {
        case th: Throwable => log.error(th, s"Can't close resource stream: $name")
      }
    }
    properties
  }

  private def buildDef(key: String, cfg: Config)(implicit log: LoggingAdapter) = {
    val propertiesFileName = get[String](Properties)
      .getOrElse(throw InitializableError(s"Missing properties file name under definition key:$key"))

    Def(
      get[Boolean](Producer).getOrElse(false),
      get[Boolean](Consumer).getOrElse(false),
      loadProperties(propertiesFileName),
      get[Boolean](Default).getOrElse(false),
      key :: get[List[String]](Aliases).getOrElse(List.empty)
    )
  }


  override def dispose(ctx: Context) = {
    val err = DisposableError("Can't dispose some kafka instances")
    val res = ctx.registered[KC].map { p =>
      Try {
        p._1.unsubscribe()
        p._1
      }
    }
    res.foldLeft(err) { (ex, v) =>
      v match {
        case Failure(th) => err.addSuppressed(th); err
        case _ => err
      }
    }

    if (err.getSuppressed.length == 0) Success(()) else Failure.apply[Unit](err)
  }
}