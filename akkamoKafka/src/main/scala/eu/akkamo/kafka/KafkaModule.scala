package eu.akkamo.kafka

import java.io.{File, FileInputStream, InputStream}
import java.util.{Properties, UUID}
import java.util.concurrent.TimeUnit

import com.typesafe.config.{Config, ConfigFactory, ConfigValue}
import eu.akkamo.Initializable.Builder
import eu.akkamo.m.config.Transformer
import eu.akkamo.{InitializableError, _}
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.common.serialization.Deserializer

import scala.util.{Failure, Success, Try}

/**
  * Simple factory serving instances of `KafKaConsumer`.
  * This API is low level, so user is responsible to close consumer instances. Also `KafkaConsumer` is not thread safe
  * @param properties
  */
class KafkaConsumerFactory(properties:Properties) {

  /**
    *
    * @tparam K type of key
    * @tparam V type of value
    * @return  created instance of `KafkaConsumer`
    */
  def apply[K,V]() = new KafkaConsumer[K, V](properties)

  /**
    *
    * @param keyDeserializer instance of deserializer
    * @param valueDeserializer instance of serializer
    * @tparam K type of key
    * @tparam V type of value
    * @return  created instance of `KafkaConsumer`
    */
  def apply[K,V](keyDeserializer: Deserializer[K], valueDeserializer: Deserializer[V]) =
      new KafkaConsumer[K, V](properties, keyDeserializer, valueDeserializer)
}

object ManagedKafkaConsumer {

  /**
    * register `KafkaConsumer` to the `Context`, registered instance isunsuscribed during shutd down phase
    * @param consumer
    * @param ctx
    * @tparam K
    * @tparam V
    * @return new instance of `Context`
    */
  def apply[K,V](consumer:KafkaConsumer[K,V])(implicit ctx:Context):Context = {
    val key = BigInt(UUID.randomUUID().toString.replace("-", ""), 16).toString(36)
    ctx.register(consumer, key)
  }

}

/**
  * = Configuration example =
  * {{{
  *   akkamo.kafka = {
  *     // complete configuration with several name aliases
  *     name1 = {
  *       aliases = ["alias1", "alias2"]
  *       propertyFile = "name1.properties" // path to property file
  *     },
  *     // configuration registered as default (only one instance is allowed)
  *     name2 = {
  *       default = true
  *       properties = {
  *         bootstrap.servers="localhost:8000"
  *       }
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

  private case class Configuration(producer: Boolean, consumer: Boolean, propertyFile: Option[String], properties: Option[Properties])


  private val CfgKey = "akkamo.kafka"

  val default =
    s"""
       |$CfgKey = {
       | default = { }
       |}
    """.stripMargin


  override def initialize(ctx: Context) = {

    val log = ctx.get[LoggingAdapterFactory].apply(getClass)
    log.info("Initializing 'Kafka' module...")

    implicit val cfg = ctx.get[Config]

    val cpb = cpBuilder(loadProperties("kafka-default.properties"))

    val parsed: List[Initializable.Parsed[(Option[KafkaConsumerFactory], Option[KafkaProducer[_, _]])]] =
      Initializable.parseConfig(CfgKey, cfg, cpb).getOrElse {
        Initializable.parseConfig(CfgKey, ConfigFactory.parseString(default), cpb).get
      }

    Initializable.defaultReport(CfgKey, parsed)

    val consumers = parsed.filter(_._3._1.isDefined).map { case (a, b, (c, _)) => (a, b, c.get) }
    val producers = parsed.filter(_._3._2.isDefined).map { case (a, b, (_, p)) => (a, b, p.get) }
    ctx.register(consumers).register(producers)
  }

  private def cpBuilder(defaultProperties: Properties): Builder[(Option[KafkaConsumerFactory], Option[KafkaProducer[_, _]])] = (v: ConfigValue) => {
    val t: Transformer[Configuration] = implicitly[Transformer[Configuration]]
    val cfg = t(v)
    val finalProps = new Properties()
    finalProps.putAll(defaultProperties) // put default
    finalProps.putAll(cfg.properties.getOrElse(new Properties())) // put from properties section
    finalProps.putAll(cfg.propertyFile.map(loadProperties).getOrElse(new Properties())) // put from
    val consumer = if (cfg.consumer) Some(new KafkaConsumerFactory(finalProps)) else None
    val producer = if (cfg.producer) Some(new KafkaProducer(finalProps)) else None
    (consumer, producer)
  }

  private val loadProperties = (path: String) => {
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
      case None => new Properties()
      case Some(stream) =>
        val properties: Properties = new Properties()
        try {
          properties.load(stream)
        } catch {
          case th: Throwable => throw InitializableError(s"Can't read properties from: $path", th)
        } finally {
          if (stream != null) try {
            stream.close()
          } catch {
            case _: Throwable => // ignore this
          }
        }
        properties
    }
  }

  override def dispose(ctx: Context) = {

    // error handler
    val eh = (ex: DisposableError, v: Try[Unit]) => v match {
      case Failure(th) => ex.addSuppressed(th); ex
      case _ => ex
    }

    val err = DisposableError("Can't dispose some kafka instances")

    // TODO async dispose ?
    ctx.registered[KafkaProducer[_, _]].map { p =>
      Try {
        // close operation is blocking
        // TODO - put timeout to config
        p._1.close(60, TimeUnit.SECONDS)
      }
    }.foldLeft(err)(eh)

    ctx.registered[KafkaConsumer[_, _]].map { p =>
      Try {
        p._1.unsubscribe()
      }
    }.foldLeft(err)(eh)


    // return error if not empty suppressed
    if (err.getSuppressed.length == 0) Success(()) else Failure.apply[Unit](err)
  }

  override def dependencies(ds: TypeInfoChain): TypeInfoChain = ds.&&[LoggingAdapterFactory].&&[Config]

  override def publish(ds: TypeInfoChain): TypeInfoChain = ds.&&[KafkaConsumerFactory].&&[KafkaProducer[_, _]]
}