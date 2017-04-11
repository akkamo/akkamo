package eu.akkamo

import akka.actor.ActorSystem
import com.typesafe.config.Config

import scala.collection.mutable
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

  /**
    * pointer to array containing set of akka Actor System names in configuration
    */
  val AkkaSystemsKey = "akkamo.akka"

  /**
    * in concrete akka config points to array of aliases
    */
  val Aliases = "aliases"

  /**
    * the name of actor system
    */
  val Name = "name"

  val actorSystems = mutable.Set.empty[ActorSystem]

  /**
    * Initializes the module into provided mutable context, blocking
    */
  override def initialize(ctx: Context) = Try {
    import config._
    val cfg = ctx.inject[Config].get
    get[Map[String, Config]](AkkaSystemsKey, cfg).fold {
      // empty configuration just create default
      try {
        ctx.register(ActorSystem("default"))
      } catch {
        case th: Throwable => throw InitializableError("Can't initialize default Akka system", th)
      }
    } { bloks =>
      val bloksWithDefault = if (bloks.size == 1) {
        bloks.map(p => (p._1, p._2, true))
      } else {
        val ret = bloks.map { case (key, cfg) =>
          val default = cfg.hasPath("default") && cfg.getBoolean("default")
          (key, cfg, default)
        }
        val defaultCount = ret.count({ case (_, _, x) => x })
        if (defaultCount != 1) {
          throw InitializableError(s"In akka module configuration found ${defaultCount} of blocks having default=true. Only one such value is allowed.")
        }
        ret
      }
      bloksWithDefault.foldLeft(ctx) { case (ctx, (key, cfg, default)) =>
        try {
          import config._
          val system = ActorSystem(key, cfg)
          actorSystems += system
          // register default
          val ctx2 = if (default) {
            ctx.register(system)
          } else {
            ctx
          }
          // register under key as name
          val ctx3 = ctx2.register(system, Some(key))
          // register rest
          config.get[List[String]](Aliases, cfg).map(_.foldLeft(ctx3) {
            case (ctx, name) => ctx.register(system, Some(name))
          }).getOrElse(ctx3)
        } catch {
          case th: Throwable => throw InitializableError(s"Can't initialize Akka system defined by: ${key}", th)
        }
      }
    }
  }

  @throws[DisposableError]("If dispose execution fails")
  override def dispose(ctx: Context) = {
    import scala.concurrent.ExecutionContext.Implicits.global
    val futures = actorSystems.map(p => p.terminate.transform(p => p, th => DisposableError(s"Can't initialize route ${p}", th)))
    Future.sequence(futures).map { p => () }
  }

  override def dependencies(dependencies: Dependency): Dependency = dependencies.&&[Config]


  override def publish(): Set[Class[_]] = Set(classOf[ActorSystem])
}
