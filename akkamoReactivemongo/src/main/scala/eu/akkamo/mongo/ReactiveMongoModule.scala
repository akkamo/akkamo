package eu.akkamo.mongo

import com.typesafe.config.{Config, ConfigFactory}
import eu.akkamo._
import eu.akkamo.config._
import reactivemongo.api.{DB, DBMetaCommands, MongoConnection, MongoDriver}

import scala.collection.Map
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.Try

/**
  * For each configured ''ReactiveMongo'' connection, the instance of this trait is registered into
  * the ''Akkamo'' context and provides API for accessing the connection.
  */
trait ReactiveMongoApi {

  /**
    * Reference to the ''ReactiveMongo'' driver instance for the current connection.
    *
    * @return ''ReactiveMongo'' driver instance
    */
  def driver: MongoDriver

  /**
    * Reference to the ''ReactiveMongo'' connection pool instance.
    *
    * @return ''ReactiveMongo'' connection pool instance
    */
  def connection: MongoConnection

  /**
    * Reference to the ''ReactiveMongo'' database instance.
    *
    * @return ''ReactiveMongo'' database instance
    */
  def db: DB with DBMetaCommands
}

/**
  * ''Akkamo module'' providing support for ''MongoDB'' via the ''ReactiveMongo'' driver.
  *
  * = Example configuration =
  * {{{
  *   akkamo.reactiveMongo {
  *
  *    	// ReactiveMongo loads its configuration from here (e.g. configuration of the
  *    	// underlying Akka system)
  *    	akka {
  *      		loglevel = WARNING
  *    	}
  *     name1 = {
  *       aliases=["alias1", "alias2"]
  *       default = true // required if more than one reactive mongo configuration exists
  *       uri = "mongodb://someuser:somepasswd@localhost:27017/your_db_name"
  *     }
  *     name2 = {
  *       aliases=["alias1", "alias2"]
  *       uri = "mongodb://someuser:somepasswd@localhost:27017/your_db_name"
  *     }
  *   }
  * }}}
  *
  * Above is the working example of simple module configuration. In this configuration example,
  * one ''ReactiveMongo'' connection is created and will be registered into the ''Akkamo'' context.
  * Each configured connection is registered into the ''Akkamo'' context as the
  * [[eu.akkamo.mongo.ReactiveMongoApi]] interface and is avaiable for injection via its name
  * (e.g. ''name1''), aliases (if defined, e.g. ''alias1'' or ''alias2''), or if defined as
  * ''default'' without any name identifier.
  *
  * ==If configuration is missing, then default entry with `uri: mongodb://localhost/default` is created.==
  *
  * @author vaclav.svejcar
  * @author jubu
  */
class ReactiveMongoModule extends Module with Initializable with Disposable {

  import eu.akkamo

  import scala.concurrent.ExecutionContext.Implicits.global

  val ReactiveMongoModuleKey = "akkamo.reactiveMongo"

  val AliasesKey: String = "aliases"
  val DefaultKey: String = "default"
  val UriKey: String = "uri"

  val ConnectionStartTimeout: FiniteDuration = 30.seconds
  val ConnectionStopTimeout: FiniteDuration = 10.seconds

  case class ReactiveMongo(driver: MongoDriver, connection: MongoConnection,
                           db: DB with DBMetaCommands) extends ReactiveMongoApi

  override def dependencies(dependencies: Dependency): Dependency =
    dependencies.&&[ConfigModule].&&[LogModule]

  override def initialize(ctx: Context) = {
    val cfg: Config = ctx.inject[Config].get
    val log = ctx.inject[LoggingAdapterFactory].map(_ (this)).get

    log.info("Initializing 'ReactiveMongo' module")

    val driver = new MongoDriver(get[Config](ReactiveMongoModuleKey, cfg), None)
    val ctx2 = ctx.register(driver, ReactiveMongoModuleKey)
    // we must remove driver by hands if something goes wrong
    registerConnections(ctx2, cfg).transform(identity, {th=> Try(driver.close()); th})
  }

  override def dispose(ctx: Context) = Try {
    val log = ctx.inject[LoggingAdapterFactory].map(_ (this)).get
    log.info("Dispose 'ReactiveMongo' module")
    ctx.inject[MongoDriver](ReactiveMongoModuleKey).foreach(_.close())
    ()
  }

  private def parseConfig(cfg: Map[String, Config]): List[Conf] = {
    import scala.collection.JavaConversions._

    def aliases(conf: Config): Seq[String] =
      if (conf.hasPath(AliasesKey)) conf.getStringList(AliasesKey) else Seq.empty[String]

    if (cfg.size == 1) {
      val (name, config) = cfg.head
      List(Conf(name, config, default = true, aliases(config)))
    } else {
      val configs: Iterable[Conf] = cfg.map { case (key, value) =>
        val default = value.hasPath(DefaultKey) && value.getBoolean(DefaultKey)
        Conf(key, value, default = default, aliases(value))
      }

      val defaults: Int = configs count (_.default)
      if (defaults != 1) {
        throw InitializableError(s"Found $defaults default config blocks in module " +
          s"configuration. Only one module can be declared as default.")
      }
      configs.toList
    }
  }

  private def registerConnections(ctx: Context, cfg: Config) = {
    val driver = ctx.get[MongoDriver](ReactiveMongoModuleKey)
    val configMap = akkamo.config.blockAsMap(ReactiveMongoModuleKey)(cfg).getOrElse(makeDefault)
    parseConfig(configMap).foldLeft(Future.successful(ctx)) { case (ctxFt, conf) =>
      val connFt: Future[ReactiveMongo] = wrapInitErr(createConnection(driver, conf),
        th => s"Cannot create Mongo connection for '${conf.name}' configuration")

      for {
        conn <- connFt
        ctx1 <- ctxFt
      } yield {
        // register configuration for the name
        val ctx2 = ctx.register[ReactiveMongoApi](conn, Some(conf.name))

        // register configuration as default if necessary
        val ctx3 = if (conf.default) ctx2.register[ReactiveMongoApi](conn) else ctx2

        // register configuration for defined aliases
        conf.aliases.foldLeft(ctx3)((ct, alias) => ct.register[ReactiveMongoApi](conn, Some(alias)))
      }
    }
  }


  private def createConnection(driver: MongoDriver, config: Conf): Future[ReactiveMongo] = {
    for {
      uri <- Future.fromTry(MongoConnection.parseURI(config.config.getString(UriKey)))
      conn = driver.connection(uri)
      dbName <- Future(uri.db.get)
      db <- conn.database(dbName)
    } yield ReactiveMongo(driver, conn, db)
  }

  private def makeDefault: Map[String, Config] = Map(
    "default" -> ConfigFactory.parseString("""uri = "mongodb://localhost/default"	""".stripMargin))


  private def wrapErr[T](err: (String, Throwable) => Throwable)
                        (ft: Future[T], msg: Throwable => String): Future[T] = {
    ft.transform(x => x, th => err(msg(th), th))
  }

  private def wrapInitErr[T] = wrapErr[T](InitializableError) _

  private case class Conf(name: String, config: Config, default: Boolean, aliases: Seq[String])

}
