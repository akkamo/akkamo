package eu.akkamo.mongo

import com.mongodb.ConnectionString
import com.typesafe.config.{Config, ConfigFactory}
import eu.akkamo.{Context, Disposable, Initializable, LoggingAdapter, LoggingAdapterFactory, Module, Publisher, TypeInfoChain}
import org.mongodb.scala.{MongoClient, MongoDatabase}

import scala.util.Try


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

  import eu.akkamo.m.config._ // need by parseConfig

  private class MongoApiImpl(uri: String) extends MongoApi {

    override def client: MongoClient = MongoClient(uri)

    override def db: MongoDatabase = client.getDatabase(new ConnectionString(uri).getDatabase)
  }


  val CfgKey = "akkamo.mongo"

  val default =
    s"""
       |$CfgKey = {
       | default = {
       |  uri = "mongodb://localhost/default"
       | }
       |}
    """.stripMargin

  override def dependencies(dependencies: TypeInfoChain): TypeInfoChain = dependencies.&&[Config].&&[LoggingAdapterFactory]

  override def publish(ds: TypeInfoChain): TypeInfoChain = ds.&&[MongoApi]

  override def initialize(ctx: Context) = Try {
    val log = ctx.get[LoggingAdapterFactory].apply(getClass)
    log.info("Initializing 'MongoDB' module...")

    val cfg = ctx.get[Config]

    val registered: List[Initializable.Parsed[MongoApi]] =
      Initializable.parseConfig[MongoApiImpl](CfgKey, cfg).getOrElse {
        Initializable.parseConfig[MongoApiImpl](CfgKey, ConfigFactory.parseString(default)).get
      }
    ctx.register(Initializable.defaultReport(CfgKey, registered))
  }

  override def dispose(ctx: Context) = Try {
    val log: LoggingAdapter = ctx.get[LoggingAdapterFactory].apply(getClass)

    ctx.registered[MongoApi] foreach { case (mongoApi, aliases) =>
      log.info(s"Terminating Mongo connection registered for names: ${aliases.mkString(",")}")
      mongoApi.client.close()
    }
  }
}