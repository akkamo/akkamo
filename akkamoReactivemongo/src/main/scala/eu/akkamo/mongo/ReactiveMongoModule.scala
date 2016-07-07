package eu.akkamo.mongo
import akka.event.LoggingAdapter
import com.typesafe.config.{Config, ConfigFactory}
import eu.akkamo._
import eu.akkamo.config._
import reactivemongo.api.{DB, DBMetaCommands, MongoConnection, MongoDriver}

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
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
  * [[eu.akkamo.mongo.ReactiveMongoApi]] interface and is avaiable for injection via its name (e.g. ''name1''),
  * aliases (if defined, e.g. ''alias1'' or ''alias2''), or if defined as ''default'' without
  * any name identifier.<br/>
  * ==If configuration is missing, then default entry with `uri: mongodb://localhost/mdg` is created.==
  *
  * @author vaclav.svejcar
  * @author jubu
  */
class ReactiveMongoModule extends Module with Initializable with Disposable {

  val ReactiveMongoModuleKey = "akkamo.reactiveMongo"

  val AliasesKey: String = "aliases"
  val DefaultKey: String = "default"
  val UriKey: String = "uri"

  val ConnectionStartTimeout: FiniteDuration = 30.seconds
  val ConnectionStopTimeout: FiniteDuration = 10.seconds

  private var driver: MongoDriver = _

  case class ReactiveMongo(driver: MongoDriver, connection: MongoConnection, db: DB with DBMetaCommands)
    extends ReactiveMongoApi

  override def initialize(ctx: Context) = Try {
    val cfg = ctx.inject[Config]
    val log = ctx.inject[LoggingAdapterFactory].map(_ (this))
    initialize(ctx, cfg.get, log.get)
  }

  override def dispose(ctx: Context) = {
    val log = ctx.inject[LoggingAdapterFactory].map(_ (this)).get
    val res = Some(driver).map { driver =>
      implicit val ec = driver.system.dispatcher
      val res = ctx.registered[ReactiveMongoApi].map { case (conn, names) =>
        wrapDispErr(conn.connection.askClose()(ConnectionStopTimeout),
          th => s"Cannot stop connection for '${names}' configuration").map { p =>
          log.info(s"Mongo connection '${names}' stopped")
        }
      }
      val f = wrapDispErr(
        driver.system.terminate().map { p =>
          log.info(s"Mongo system: '${driver.system}' terminated")
        },
        th => s"Cannot stop underlying Actor system")

      Future.sequence(res ++ List(f)).map{p=>()}
    }
    val result = res.getOrElse(Future.successful(()))
    result
   }

  private def initialize(ctx: Context, cfg: Config, log: LoggingAdapter): Context = {
    import eu.akkamo

    log.info("Initializing 'ReactiveMongo' module...")

    driver = new MongoDriver(get[Config](ReactiveMongoModuleKey, cfg))

    val configMap = akkamo.config.blockAsMap(ReactiveMongoModuleKey)(cfg).getOrElse(makeDefault)
    parseConfig(configMap).foldLeft(ctx) { case (ctx, conf) =>
      log.info(s"Creating Mongo connection for '${conf.name}' configuration")

      val connFt = wrapInitErr(createConnection(conf),
        th => s"Cannot create Mongo connection for '${conf.name}' configuration")
      val conn = Await.result(connFt, ConnectionStartTimeout)

      // register configuration for the name
      val ctx1 = ctx.register[ReactiveMongoApi](conn, Some(conf.name))

      // register configuration as default if necessary
      val ctx2 = if (conf.default) {
        ctx1.register[ReactiveMongoApi](conn)
      } else {
        ctx1
      }
      // register configuration for defined aliases
      conf.aliases.foldLeft(ctx)((ctx, alias) => ctx.register[ReactiveMongoApi](conn, Some(alias)))
    }
  }

  private def makeDefault: Map[String, Config] = {
    val cfgString = """uri = "mongodb://localhost/default"	""".stripMargin
    Map("default" -> ConfigFactory.parseString(cfgString))
  }

  private def createConnection(config: Conf): Future[ReactiveMongo] = {
    implicit val ec = driver.system.dispatcher
    for {
      uri <- Future.fromTry(MongoConnection.parseURI(config.config.getString(UriKey)))
      conn = driver.connection(uri)
      dbName <- Future(uri.db.get)
      db <- conn.database(dbName)
    } yield ReactiveMongo(driver, conn, db)
  }

  private def parseConfig(cfg: Map[String, Config]): List[Conf] = {
    import scala.collection.JavaConversions._

    def aliases(conf: Config): Seq[String] = if (conf.hasPath(AliasesKey)) {
      conf.getStringList(AliasesKey)
    } else {
      Seq.empty[String]
    }

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
        throw new InitializableError(s"Found $defaults default config blocks in module " +
          s"configuration. Only one module can be declared as default.")
      }
      configs.toList
    }
  }

  private def wrapErr[T](err: (String, Throwable) => Throwable)
                        (ft: Future[T], msg: Throwable => String): Future[T] = {
    import scala.concurrent.ExecutionContext.Implicits.global
    ft.transform(x => x, th => err(msg(th), th))
  }

  private def wrapInitErr[T] = wrapErr[T](InitializableError) _

  private def wrapDispErr[T] = wrapErr[T](DisposableError) _

  private case class Conf(name: String, config: Config, default: Boolean, aliases: Seq[String])

  override def dependencies(dependencies: Dependency): Dependency =
    dependencies.&&[ConfigModule].&&[LogModule].&&[AkkaModule]
}
