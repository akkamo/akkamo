package eu.akkamo

import scala.util.{Failure, Try}

/**
  * Implementation of the Context
  */
class CTX extends Context {

  import scala.collection._
  import scala.reflect.ClassTag

  val Default = "@$DEFAULT$@"
  val class2Key2Inst = mutable.Map.empty[Class[_], mutable.Map[String, AnyRef]]

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

  override def registered[T](implicit ct: ClassTag[T]): Map[T, Set[String]] = {
    class2Key2Inst.get(ct.runtimeClass).map { m =>
      m.groupBy(_._2).map { case (k, v) =>
        (k.asInstanceOf[T], v.keySet.filter(_ != Default))
      }
    }.getOrElse(Map.empty)
  }

  override def register[T <: AnyRef](value: T, key: Option[String])(implicit ct: ClassTag[T]): Context = {
    val key2Inst = class2Key2Inst.getOrElse(ct.runtimeClass, mutable.Map.empty)
    val realKey = key.getOrElse(Default)
    if (key2Inst.contains(realKey)) {
      throw InitializationError(s"module: $value under key: $key already registered")
    }
    key2Inst += (key.getOrElse(Default) -> value)
    class2Key2Inst += (ct.runtimeClass -> key2Inst)
    this
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


class AkkamoRun(modules: List[Module]) {

  def this() {
    this(AkkamoRun.modules())
  }

  import Logger._

  def apply(implicit ctx: CTX): List[Module] = {

    log(s"Modules: $modules")

    val (_, ordered) = order(modules)

    val errors1 = init(ordered.reverse)
    // end of game
    if (!errors1.isEmpty) {
      val e = RunError(s"Some errors occurred during initialization")
      errors1.foldLeft(e) {
        case (e, (m, th)) => {
          e.addSuppressed(th);
          e
        }
      }
      Try(new AkkamoDispose(false)(ctx, modules)).failed.map { th =>
        e.addSuppressed(th)
      }
      throw e
    }

    log("Run modules: " + ordered.filter(_.isInstanceOf[Runnable]))

    val errors2 = run(ordered)
    // end of game
    if (!errors2.isEmpty) {
      val e = RunError("Some errors occurred during attempt to run installed modules")
      errors2.foldLeft(e) {
        case (e, (m, th)) => {
          e.addSuppressed(th);
          e
        }
      }
      Try(new AkkamoDispose(false)(ctx, modules)).failed.map { th =>
        e.addSuppressed(th)
      }
      throw e
    }
    ordered
  }

  private def order(in: List[Module], set: Set[Class[_]] = Set.empty, out: List[Module] = Nil)
                   (implicit ctx: CTX): (Set[Class[_]], List[Module]) = {

    def orderRound(in: List[Module], set: Set[Class[_]], out: List[Module] = Nil): (Set[Class[_]], List[Module]) = {
      val des = ctx.createDependencies(set)
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
                  (implicit ctx: Context): List[(Module, Throwable)] = in match {
    case x :: xs =>
      if (x.isInstanceOf[Initializable]) {
        Try {
          log(s"Initialising module: $x")
          x.asInstanceOf[Initializable].initialize(ctx)
        } match {
          case Failure(th) => init(xs, (x, th) :: out)
          case _ => init(xs, out)
        }
      } else {
        init(xs, out)
      }
    case _ => out
  }


  private def run(in: List[Module], out: List[(Module, Throwable)] = Nil)
                 (implicit ctx: Context): List[(Module, Throwable)] = in match {
    case x :: xs =>
      if (x.isInstanceOf[Runnable]) {
        Try {
          log(s"Running module: $x")
          x.asInstanceOf[Runnable].run(ctx)
        } match {
          case Failure(th) => run(xs, (x, th) :: out)
          case _ => run(xs, out)
        }
      } else {
        run(xs, out)
      }
    case _ => out
  }
}

object AkkamoRun {

  import scala.collection.JavaConversions._

  def modules() = java.util.ServiceLoader.load[Module](classOf[Module]).toList
}


class AkkamoDispose(verbose: Boolean) extends ((CTX, List[Module]) => Unit) {

  def this() = {
    this(true)
  }

  import Logger._

  override def apply(ctx: CTX, modules: List[Module]): Unit = {
    log("Dispose modules: " + modules.filter(_.isInstanceOf[Disposable]))
    val errors = dispose(modules)(ctx)
    // end of game
    if (!errors.isEmpty) {
      val e = InitializationError(s"Some errors occurred during disposal of modules")
      errors.foldLeft(e) {
        case (e, (m, th)) => {
          e.addSuppressed(th);
          e
        }
      }
      throw e
    }
  }

  private def dispose(in: List[Module], out: List[(Module, Throwable)] = Nil)(implicit ctx: CTX): List[(Module, Throwable)] = in match {
    case x :: xs =>
      if (x.isInstanceOf[Disposable]) {
        Try {
          log(s"Dispose module: $x")
          x.asInstanceOf[Disposable].dispose(ctx)
        } match {
          case Failure(th) => dispose(xs, (x, th) :: out)
          case _ => dispose(xs, out)
        }
      } else {
        dispose(xs, out)
      }
    case _ => out
  }

}

/**
  * @author jubu
  */
object Akkamo extends App {

  val akkamoRun = new AkkamoRun
  val akkamoDispose = new AkkamoDispose

  val errorHook = new Thread() {
    override def run() = {
      Console.err.println("An error occured during initialisation")
    }
  }
  Runtime.getRuntime.addShutdownHook(errorHook)
  val ctx: CTX = new CTX
  val modules = akkamoRun(ctx)
  Runtime.getRuntime.removeShutdownHook(errorHook)
  Runtime.getRuntime.addShutdownHook(new Thread() {
    override def run() = try {
      akkamoDispose(ctx, modules)
    } catch {
      case th: Throwable => th.printStackTrace(Console.err)
    }
  })
}

private object Logger {
  def log(message: String, asError: Boolean = false) = {
    if (System.getProperty("akkamo.verbose", "false").toBoolean) {
      if (asError) {
        Console.err.println(message)
      } else {
        println(message)
      }
    }
  }
}

/**
  *
  * @param message
  * @param cause
  */
case class InitializationError(message: String, cause: Throwable = null) extends Error(message, cause)

/**
  *
  * @param message
  * @param cause
  */
case class RunError(message: String, cause: Throwable = null) extends Error(message, cause)