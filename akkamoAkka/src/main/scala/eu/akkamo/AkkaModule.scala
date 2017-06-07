package eu.akkamo

import akka.actor.ActorSystem
import com.typesafe.config.{Config, ConfigFactory}

import scala.concurrent.Future
import scala.util.Try

/**
  * Register one or more Actor System
  * {{{
  *   configuration example:
  *   akkamo.akka = {
  *     // one block with akka configuration contains several aliases with the name name
  *     name1 = {
  *       aliases = ["alias1, "alias2"]
  *       // standard akka attributes for example:
  *      	akka{
  *      	  loglevel = "DEBUG"
  *        	debug {
  *        	  lifecycle = on
  *       	}
  *       }
  *      	// ....
  *     },
  *     name2 = { // not aliases - only one block allowed
  *       default = true
  *       ....
  *     }
  *   }
  * }}}
  * In a case when more than one akka configuration exists, one must be denoted as `default` <br/>
  * In case when missing configuration one default Akka system is created with name default.
  */
class AkkaModule extends Module with Initializable with Disposable with Publisher {

  import config.implicits._

  /**
    * pointer to array containing set of akka Actor System names in configuration
    */
  val CfgKey = "akkamo.akka"

  val default =
    s"""
       |$CfgKey = {
       | system = {}
       |}
    """.stripMargin


  /**
    * Initializes the module into provided mutable context, blocking
    */
  override def initialize(ctx: Context) = Try {
    implicit val cfg = ctx.get[Config]
    val registered = Initializable.parseConfig[Config](CfgKey).getOrElse {
      Initializable.parseConfig[Config](CfgKey, ConfigFactory.parseString(default)).get
    }.map { case (default, alliases, cfg) => (default, alliases, ActorSystem.apply(alliases.head, cfg)) }

    ctx.register(Initializable.defaultReport(CfgKey, registered))
  }

  @throws[DisposableError]("If dispose execution fails")
  override def dispose(ctx: Context) = {
    import scala.concurrent.ExecutionContext.Implicits.global
    val futures = ctx.registered[ActorSystem].map { case (s, _) => s.terminate() }
    Future.sequence(futures).map { p => () }
  }

  override def dependencies(dependencies: TypeInfoChain): TypeInfoChain = dependencies.&&[Config]

  override def publish(dependency: TypeInfoChain): TypeInfoChain = dependency.&&[ActorSystem]

}
