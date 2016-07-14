package eu.akkamo

import scala.collection.Set
import scala.reflect.ClassTag
import scala.util.{Failure, Success, Try}

/**
  * Implementation of the Context
  */
case class CTX(class2Key2Inst: Map[Class[_], Map[String, AnyRef]] = Map.empty) extends Context {

  import scala.collection._
  import scala.reflect.ClassTag

  val Default = "@$DEFAULT$@"

  override def inject[T](implicit ct: ClassTag[T]): Option[T] = {
    inject(Default)
  }

  override def inject[T](key: String, strict: Boolean = false)(implicit ct: ClassTag[T]): Option[T] = {
    class2Key2Inst.get(ct.runtimeClass).flatMap { p =>
      val ret = p.get(key)
      if (ret.isEmpty && !strict) {
        p.get(Default)
      } else {
        ret
      }
    }.map(_.asInstanceOf[T])
  }

  override def registerIn[T <: Registry[X], X](x: X, key: Option[String])(implicit ct: ClassTag[T]): Context = {
    val k = key.getOrElse(Default)
    val injected = inject[T](k).getOrElse(throw new ContextError(s"Can't find instance of: ${ct.runtimeClass.getName} for key: $key"))
    val keys = registered[T].get(injected).get // at least one element must be defined
    val updated = injected.copyWith(x).asInstanceOf[T] // is this construct ok in any case ?
    keys.foldLeft(unregisterInternal(ct.runtimeClass, keys + Default)) { (ctx, name) =>
      ctx.registerInternal(updated, ct.runtimeClass, Some(name), false)
    }
  }

  override def registered[T <: AnyRef](implicit ct: ClassTag[T]): Map[T, Set[String]] = {
    class2Key2Inst.get(ct.runtimeClass).map { m =>
      m.groupBy(_._2).map { case (k, v) =>
        (k.asInstanceOf[T], v.keySet.filter(_ != Default))
      }
    }.getOrElse(Map.empty)
  }

  override def register[T <: AnyRef](value: T, key: Option[String])(implicit ct: ClassTag[T]): Context = {
    registerInternal(value, ct.runtimeClass, key)
  }

  private def registerInternal(value: AnyRef, clazz: Class[_], key: Option[String], public: Boolean = true)(): CTX = {
    val key2Inst = class2Key2Inst.getOrElse(clazz, Map.empty)
    val realKey = key.getOrElse(Default)
    if (key2Inst.contains(realKey) && public) {
      throw ContextError(s"module: $value under key: $key already registered")
    }
    val resKey2Inst = key2Inst + (key.getOrElse(Default) -> value)
    val res = class2Key2Inst + (clazz -> resKey2Inst)
    this.copy(res)
  }

  private def unregisterInternal(clazz: Class[_], keys: Set[String]): CTX = {
    val key2Inst = class2Key2Inst.getOrElse(clazz, Map.empty)
    val resKey2Inst = key2Inst -- keys
    val res = class2Key2Inst + (clazz -> resKey2Inst)
    this.copy(res)
  }
}


class Akkamo {

  import Logger._

  type AkkamoData = (Context, List[Module])

  def run(): AkkamoData = {
    run(new CTX, Akkamo.modules())
  }

  def run(ctx: Context, modules: List[Module]): AkkamoData = {

    log(s"Modules: $modules")

    val (_, ordered) = order(modules)

    val (ctx1, errors1) = init(ordered.reverse)(ctx)
    // end of game
    if (!errors1.isEmpty) {
      val e = InitializationError(s"Some errors occurred during initialization")
      errors1.foldLeft(e) {
        case (e, (m, th)) => {
          e.addSuppressed(th);
          e
        }
      }
      Try(dispose((ctx1, modules))).failed.map { th =>
        e.addSuppressed(th)
      }
      throw e
    }

    log("Run modules: " + ordered.filter(_.isInstanceOf[Runnable]))

    val (ctx2, errors2) = run(ordered)(ctx1)
    // end of game
    if (!errors2.isEmpty) {
      val e = RunError("Some errors occurred during attempt to run installed modules")
      errors2.foldLeft(e) {
        case (e, (m, th)) => {
          e.addSuppressed(th);
          e
        }
      }
      Try(dispose((ctx2, modules))).failed.map { th =>
        e.addSuppressed(th)
      }
      throw e
    }
    (ctx2, ordered)
  }

  private def order(in: List[Module], set: Set[Class[_]] = Set.empty, out: List[Module] = Nil): (Set[Class[_]], List[Module]) = {

    def orderRound(in: List[Module], set: Set[Class[_]], out: List[Module] = Nil): (Set[Class[_]], List[Module]) = {
      val des = createDependencies(set)
      in match {
        case x :: xs => {
          val r = x.dependencies(des)
          if (r()) {
            if (x.isInstanceOf[Initializable]) {
              orderRound(xs, set + x.asInstanceOf[Initializable].iKey, x :: out)
            } else {
              orderRound(xs, set + x.getClass, x :: out)
            }
          } else {
            orderRound(xs, set, out)
          }
        }
        case _ => (set, out)
      }
    }

    val (rset, rout) = orderRound(in, set)
    if (in.isEmpty) {
      (rset, rout ++ out)
    } else {
      if (rout.isEmpty) {
        throw InitializationError(s"Can't initialize modules: $in, cycle or unresolved dependency detected.")
      }
      val df = in.diff(rout)
      order(df, rset, rout ++ out)
    }
  }

  private def init(in: List[Module], out: List[(Module, Throwable)] = Nil)
                  (ctx: Context): (Context, List[(Module, Throwable)]) = in match {
    case x :: xs =>
      if (x.isInstanceOf[Initializable]) {
        log(s"Initialising module: $x")
        x.asInstanceOf[Initializable].initialize(ctx).asTry() match {
          case Failure(th) => init(xs, (x, th) :: out)(ctx)
          case Success(c) => init(xs, out)(c)
        }
      } else {
        init(xs, out)(ctx)
      }
    case _ => (ctx, out)
  }


  private def run(in: List[Module], out: List[(Module, Throwable)] = Nil)
                 (implicit ctx: Context): (Context, List[(Module, Throwable)]) = in match {
    case x :: xs =>
      if (x.isInstanceOf[Runnable]) {
        log(s"Running module: $x")
        x.asInstanceOf[Runnable].run(ctx).asTry() match {
          case Failure(th) => run(xs, (x, th) :: out)(ctx)
          case Success(c) => run(xs, out)(c)
        }
      } else {
        run(xs, out)(ctx)
      }
    case _ => (ctx, out)
  }

  def dispose(data: AkkamoData): Unit = {
    val (ctx, modules) = data
    log("Dispose modules: " + modules.filter(_.isInstanceOf[Disposable]))
    val errors = dispose(modules)(ctx)
    // end of game
    if (!errors.isEmpty) {
      val e = DisposeError(s"Some errors occurred during disposal of modules")
      errors.foldLeft(e) {
        case (e, (m, th)) => {
          e.addSuppressed(th);
          e
        }
      }
      throw e
    }
  }

  private def dispose(in: List[Module], out: List[(Module, Throwable)] = Nil)(ctx: Context): List[(Module, Throwable)] = in match {
    case x :: xs =>
      if (x.isInstanceOf[Disposable]) {
        log(s"Dispose module: $x")
        x.asInstanceOf[Disposable].dispose(ctx).asTry() match {
          case Failure(th) => dispose(xs, (x, th) :: out)(ctx)
          case _ => dispose(xs, out)(ctx)
        }
      } else {
        dispose(xs, out)(ctx)
      }
    case _ => out
  }

  def createDependencies(dependencies: Set[Class[_]]): Dependency = {
    case class W(res: Boolean) extends Dependency {
      override def &&[K <: Module with Initializable](implicit ct: ClassTag[K]): Dependency = {
        W(this.res && dependencies.contains(ct.runtimeClass))
      }
    }
    W(true)
  }

}

object Akkamo {

  import scala.collection.JavaConversions._

  def modules() = java.util.ServiceLoader.load[Module](classOf[Module]).toList

}

/**
  * @author jubu
  */
object Main extends App {

  val akkamo = new Akkamo()

  import Logger._

  val hasVerbose = hasVerboseDefined
  if (!hasVerbose) setVerbose(false)

  val errorHook = new Thread() {
    override def run() = {
      log("An error occured during initialisation", true)
    }
  }
  Runtime.getRuntime.addShutdownHook(errorHook)
  val res = akkamo.run()
  Runtime.getRuntime.removeShutdownHook(errorHook)
  Runtime.getRuntime.addShutdownHook(new Thread() {
    override def run() = try {
      setVerbose(hasVerbose)
      akkamo.dispose(res)
    } catch {
      case th: Throwable => th.printStackTrace(Console.err)
    }
  })
}

private object Logger {

  private val Verbose = "akkamo.verbose"

  def log(message: String, asError: Boolean = false) = {
    if (isVerbose) {
      if (asError) {
        Console.err.println(message)
      } else {
        println(message)
      }
    }
  }

  def setVerbose(p: Boolean) = {
    System.setProperty(Verbose, p.toString)
  }

  def isVerbose = System.getProperty(Verbose, "true").toBoolean

  def hasVerboseDefined = System.getProperties.containsKey(Verbose)
}

/**
  * Error thrown when Akkamo initialization fails (e.g. one of the initialized modules throws
  * [[eu.akkamo.InitializableError]]).
  *
  * @param message detail message
  * @param cause   optional error cause
  */
case class InitializationError(message: String, cause: Throwable = null) extends Error(message, cause)

/**
  * Error thrown when Akkamo run phase fails (e.g. one of the ran modules throws
  * [[eu.akkamo.RunnableError]]).
  *
  * @param message detail message
  * @param cause   optional error cause
  */
case class RunError(message: String, cause: Throwable = null) extends Error(message, cause)

/**
  * Error thrown when Akkamo dispose phase fails (e.g. one of the disposed modules throws
  * [[eu.akkamo.DisposableError]]).
  *
  * @param message detail message
  * @param cause   optional error cause
  */
case class DisposeError(message: String, cause: Throwable = null) extends Error(message, cause)