package eu.akkamo.cassandra

import akka.event.LoggingAdapter
import com.typesafe.config.{Config, ConfigFactory}
import eu.akkamo._

import scala.collection.Map
import scala.util.Try
import com.websudos.phantom.connectors.{ContactPoints, KeySpaceDef}
import com.websudos.phantom.dsl.Session

/**
  * ''Akkamo module'' providing support for ''Cassandra'' database using most popular Scala driver for Cassanda - Phantom from Outworkers
  * (see [[https://github.com/outworkers/phantom]]).
  *
  * = Example configuration =
  * {{{
  *   akkamo.cassandra {
  *
  *     connection1 {
  *       default = true
  *       host = ["server1","server2"]
  *       port = 9099
  *       username = "casandra"
  *       password = "cassandrasPassword"
  *       keyspace = "mykeyspace"
  *     }

  *   }
  * }}}
  *
  * Above is the working example of simple module configuration. In this configuration example,
  * with songle connection to ''Cassandra'' created and registered into the ''Akkamo'' context.
  * Each configured connection is registeres into the ''Akkamo'' context Phantom´s drivers
  * ''KeySpaceDef'' interface to be used for data manipulation in Cassandra.
  *
  */
class CassandraModule extends Module with Initializable with Disposable {

  object Keys {
        val Aliases = "aliases"
        val Default = "default"
        val Hosts = "hosts"
        val Username = "username"
        val Password = "password"
        val Port = "port"
        val Keyspace = "keyspace"
    val ConfigNamespace = "akkamo.cassandra"
  }

  override def dependencies(dependencies: Dependency): Dependency = dependencies
    .&&[ConfigModule].&&[LogModule]

  private def defaultConfig: Map[String, Config] = Map(
    "default" -> ConfigFactory.parseString(
      """
        |hosts = ["localhost"]
        |port = 9042
        |keyspace = "akkamo"
        |username = "cassandra"
        |password = "cassandra" """.stripMargin))

  override def initialize(ctx: Context): Res[Context] = Try {
    import eu.akkamo.config.blockAsMap
    val cfg: Config = ctx.get[Config]
    val log: LoggingAdapter = ctx.get[LoggingAdapterFactory].apply(getClass)

    log.info("Initializing 'Cassandra' module...")
    val configMap = blockAsMap(Keys.ConfigNamespace)(cfg).getOrElse(defaultConfig)

    parseConfig(configMap).foldLeft(ctx) { case (context, conn) =>
      log.info(s"Initializing Cassandra session for name '${conn.name}'")
//      val mongoApi: MongoApi = createMongoApi(conn)

      val connector = ContactPoints(conn.hosts, conn.port).withClusterBuilder(
        _.withCredentials(conn.username, conn.password)).keySpace(conn.keyspace)
      val session = connector.session
      log.info(s"Connected to Cassandra ${session}")

      // create state builder providing safe way of altering immutable context
      val stateB = new StateBuilder[Context](context)

      // register the MongoApi for the specified connection name
      stateB.next(_.register[KeySpaceDef](connector, Some(conn.name)))

      // register the MongoApi as default (if necessary)
      if (conn.default) stateB.next(_.register[KeySpaceDef](connector))

      // register the MongoApi for all specified connection aliases
      conn.aliases foreach (alias => stateB.next(_.register[KeySpaceDef](connector, Some(alias))))

      stateB.value
    }


  }

  private class StateBuilder[T](initial: T) {
    private var state = initial

    def next(f: T => T): StateBuilder[T] = {
      state = f(state)
      this
    }

    def value: T = state
  }


  private case class Connection(name: String, aliases: Seq[String], default: Boolean, hosts: Seq[String], port: Int,
                                keyspace: String, username:String, password:String)

  private def parseConfig(cfg: Map[String, Config]): List[Connection] = {
    import scala.collection.JavaConversions._

    def aliases(conf: Config): Seq[String] =
      if (conf.hasPath(Keys.Aliases)) conf.getStringList(Keys.Aliases) else Seq.empty[String]
    def hosts(conf: Config): Seq[String] =
      if (conf.hasPath(Keys.Hosts)) conf.getStringList(Keys.Hosts) else Seq.empty[String]

    val connections: Iterable[Connection] = cfg map { case (key, value) =>
      val default: Boolean =
        if (cfg.size == 1) true else value.hasPath(Keys.Default) && value.getBoolean(Keys.Default)
      Connection(key, aliases(value), default, hosts(value), value.getInt(Keys.Port), value.getString(Keys.Keyspace),
        value.getString(Keys.Username),value.getString(Keys.Password))
    }

    val defaultsNo = connections count (_.default)
    if (defaultsNo > 1) throw InitializableError(s"Found $defaultsNo default config blocks in" +
      s"module configuration. Only one module can be declared as default.")

    connections.toList
  }

  override def dispose(ctx: Context): Res[Unit] = Try {
    val log: LoggingAdapter = ctx.get[LoggingAdapterFactory].apply(getClass)
    ctx.registered[Session].filter(!_._1.isClosed).foreach(_._1.close())
  }
}
