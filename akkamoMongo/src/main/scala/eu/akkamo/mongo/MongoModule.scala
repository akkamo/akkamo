package eu.akkamo.mongo

import com.mongodb.ConnectionString
import com.typesafe.config.{Config, ConfigFactory}
import eu.akkamo._
import org.mongodb.scala.{MongoClient, MongoDatabase}

import scala.collection.Map
import scala.util.Try

/**
  * ''Akkamo module'' providing support for ''MongoDB'' database using the official Scala driver
  * (see [[http://mongodb.github.io/mongo-scala-driver/]]).
  *
  * = Example configuration =
  * {{{
  *   akkamo.mongo {
  *
  *     name1 {
  *       aliases = [ "alias1", "alias2" ]
  *       default = true
  *       uri = "mongodb://user:password@localhost:27017/db_name"
  *     }
  *
  *     name2 {
  *       aliases = [ "alias3" ]
  *       uri = "mongodb://user:password@localhost:27017/db_name2"
  *     }
  *   }
  * }}}
  *
  * Above is the working example of simple module configuration. In this configuration example,
  * one ''MongoDB'' connection is created and will be registered into the ''Akkamo'' context.
  * Each configured connection is registered into the ''Akkamo'' context as the
  * [[eu.akkamo.mongo.MongoApi]] interface and is available for injection via its name
  * (e.g. ''name1''), aliases (if defined, e.g. ''alias1'' or ''alias2''), or if defined as
  * ''default'' without any name identifier.
  *
  * For more details about the URI connection string format, see the
  * [[https://docs.mongodb.com/manual/reference/connection-string/ official documentation]].
  *
  * @author Vaclav Svejcar (vaclav.svejcar@gmail.com)
  */
class MongoModule extends Module with Initializable with Disposable with Publisher {

  object Keys {
    val Aliases = "aliases"
    val Default = "default"
    val Uri = "uri"
    val ConfigNamespace = "akkamo.mongo"
  }

  override def dependencies(dependencies: Dependency): Dependency = dependencies
    .&&[Config].&&[LoggingAdapterFactory]

  override def publish(): Set[Class[_]] = Set(classOf[MongoApi])

  override def initialize(ctx: Context): Res[Context] = Try {
    import eu.akkamo.config.blockAsMap
    val cfg: Config = ctx.get[Config]
    val log = ctx.get[LoggingAdapterFactory].apply(getClass)

    log.info("Initializing 'MongoDB' module...")
    val configMap = blockAsMap(Keys.ConfigNamespace)(cfg).getOrElse(defaultConfig)
    parseConfig(configMap).foldLeft(ctx) { case (context, conn) =>
      log.info(s"Initializing Mongo connection for name '${conn.name}'")
      val mongoApi: MongoApi = createMongoApi(conn)

      val ctx1 = context.register[MongoApi](mongoApi, Some(conn.name))

      val ctx2 = if (conn.default) ctx1.register[MongoApi](mongoApi) else ctx1

      conn.aliases.foldLeft(ctx2) {(ctx, alias) => ctx.register[MongoApi](mongoApi, Some(alias)) }
    }
  }

  override def dispose(ctx: Context): Res[Unit] = Try {
    val log: LoggingAdapter = ctx.get[LoggingAdapterFactory].apply(getClass)

    ctx.registered[MongoApi] foreach { case (mongoApi, aliases) =>
      log.info(s"Terminating Mongo connection registered for names: ${aliases.mkString(",")}")
      mongoApi.client.close()
    }
  }

  private def createMongoApi(conn: Connection): MongoApi = new MongoApi {
    override def client: MongoClient = MongoClient(conn.uri)

    override def db: MongoDatabase = client.getDatabase(new ConnectionString(conn.uri).getDatabase)
  }

  private def parseConfig(cfg: Map[String, Config]): List[Connection] = {
    import scala.collection.JavaConversions._

    def aliases(conf: Config): Seq[String] =
      if (conf.hasPath(Keys.Aliases)) conf.getStringList(Keys.Aliases) else Seq.empty[String]

    val connections: Iterable[Connection] = cfg map { case (key, value) =>
      val default: Boolean =
        if (cfg.size == 1) true else value.hasPath(Keys.Default) && value.getBoolean(Keys.Default)
      Connection(key, aliases(value), default, value.getString(Keys.Uri))
    }

    val defaultsNo = connections count (_.default)
    if (defaultsNo > 1) throw InitializableError(s"Found $defaultsNo default config blocks in" +
      s"module configuration. Only one module can be declared as default.")

    connections.toList
  }

  private def defaultConfig: Map[String, Config] = Map(
    "default" -> ConfigFactory.parseString("""uri = "mongodb://localhost/default"	""".stripMargin))


  private case class Connection(name: String, aliases: Seq[String], default: Boolean, uri: String)

}

/**
  * For each configured ''MongoDB'' connection, instance of this trait is registered into the
  * ''Akkamo context'' and provides API for working with the connection.
  */
trait MongoApi {

  /**
    * Instance of `MongoConnection` for the configured ''MongoDB'' connection (see the
    * [[http://mongodb.github.io/mongo-scala-driver/1.1/scaladoc/#org.mongodb.scala.MongoClient official Scaladoc]]
    * for further details).
    *
    * @return instance of `MongoConnection`
    */
  def client: MongoClient

  /**
    * Instance of `MongoDatabase`, representing the specific ''MongoDB'' database (see the
    * [[http://mongodb.github.io/mongo-scala-driver/1.1/scaladoc/#org.mongodb.scala.MongoDatabase official Scaladoc]]
    * for further details).
    *
    * @return instance of `MongoDatabase`
    */
  def db: MongoDatabase
}
