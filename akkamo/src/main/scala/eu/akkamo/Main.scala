package eu.akkamo

import java.util.concurrent.atomic.AtomicReference

import scala.reflect.ClassTag
import scala.util.{Failure, Success, Try}

/**
  * Implementation of the Context
  */
private[akkamo] case class CTX(class2Key2Inst: Map[Class[_], Map[String, AnyRef]] = Map.empty) extends Context {

  import scala.collection._
  import scala.reflect.ClassTag

  val Default = "_@_"

  // register newly created context as last
  CTX.last.set((this, Thread.currentThread().getStackTrace))

  /**
    * Get service registered in ''Akkamo'' context, using the ''alias''.<br/>
    * Service alias must be defined in configuration.
    *
    * @param alias mapping identifier
    * @param ct    implicit ClassTag definition
    * @tparam T type of the service
    * @return implementation of interface `T` encapsulated in `Some` if exists else `None`
    */
  override def getOpt[T](alias: String)(implicit ct: ClassTag[T]) = {
    getInternal(alias, ct)
  }

  /**
    * Get service registered in ''Akkamo'' context, using the ''alias''.<br/>
    * Service alias is volatile in this case. If missing, the default value is used
    *
    * @param alias
    * @param ct
    * @tparam T
    * @return implementation of interface `T` encapsulated in `Some` if exists else `None`
    */
  override def getOpt[T](alias: Option[String] = None)(implicit ct: ClassTag[T]): Option[T] = {
    getInternal(alias.getOrElse(Default), ct) match {
      case None => getInternal(Default, ct)
      case x => x
    }
  }

  @inline
  private  def getInternal[T](key: String,  ct: ClassTag[T]): Option[T] = {
    class2Key2Inst.get(ct.runtimeClass).flatMap(_.get(key)).asInstanceOf[Option[T]]
  }


  override def registerIn[T <: Registry[X], X](x: X, key: Option[String])(implicit ct: ClassTag[T]): Context = {
    val injected = getOpt[T](key).getOrElse(throw new ContextError(s"Can't find instance of: ${ct.runtimeClass.getName} for alias: $key"))
    val keys = registeredInternal[T].get(injected).get // at least one element must be defined
    val updated = injected.copyWith(x).asInstanceOf[T] // is this construct ok in any case ?
    keys.foldLeft(unregisterInternal(ct.runtimeClass, keys)) { (ctx, name) =>
      ctx.registerInternal(updated, Some(name), false)
    }
  }

  override def register[T <: AnyRef](value: T, key: Option[String])(implicit ct: ClassTag[T]): Context = {
    registerInternal(value, key)
  }

  override def registered[T <: AnyRef](implicit ct: ClassTag[T]): Map[T, Set[String]] = {
    registeredInternal[T].map { case (k, v) =>
      (k, v-Default)
    }
  }


  private def registeredInternal[T<:AnyRef](implicit ct: ClassTag[T]): Map[T, Set[String]] = {
    class2Key2Inst.get(ct.runtimeClass).map { m =>
      m.groupBy(_._2).map { case (k, v) =>
        (k.asInstanceOf[T], v.keySet)
      }
    }.getOrElse(Map.empty)
  }

  private def registerInternal[T <: AnyRef](value: T, key: Option[String], public: Boolean = true)(implicit ct: ClassTag[T]): CTX = {
    val clazz = ct.runtimeClass
    val key2Inst = class2Key2Inst.getOrElse(clazz, Map.empty)
    val realKey = key.getOrElse(Default)
    if (key2Inst.contains(realKey) && public) {
      throw ContextError(s"module: ${value} under alias: ${key} already registered")
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

private[akkamo] object CTX {
  /**
    *
    * keep last created context in local thread variable
     */
  private[CTX] val last  = new AtomicReference[(Context, Array[StackTraceElement])]()

  /**
    * compare `ctx` with last created Context
    * @param ctx
    * @return true if `ctx` eq last.get
    */
  def isLast(ctx:Context) = getContext.map(_ eq ctx).getOrElse(false)

  /**
    *
    * @return  formatted information about place when the problem happened
    */
  def getInvocationInfo = getStackTrace.map{p=>
    val el = p.toSeq.tail.filterNot(filter).headOption
    el.map(el=>s"(${el.getFileName}${el.getClassName}:${el.getMethodName} at line ${el.getLineNumber})").getOrElse("unknown location")
  }

  private def getContext = Option(last.get()).map(_._1)
  private def getStackTrace = Option(last.get()).map(_._2)

  private val names = Set(classOf[CTX].getName)
  private val filter = (e:StackTraceElement) => names.contains(e.getClassName)
}

class Akkamo {

  import Logger._

  type AkkamoData = (Context, List[Module])

  def run(): AkkamoData = {
    run(new CTX, Akkamo.modules())
  }

  def run(ctx: Context, modules: List[Module]): AkkamoData = {

    log(s"Modules: ${modules}")

    val publishers = publisherMap(modules)

    val ordered =
      order(modules, publishers)

    val (ctx1, errors1) = init(ordered.reverse)(ctx)
    // end of game
    if (!errors1.isEmpty) {
      val e = InitializationError(s"Some errors occurred during initialization")
      errors1.foldLeft(e) {
        case (e, (_, th)) => {
          e.addSuppressed(th)
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
        case (e, (_, th)) => {
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

  private def publisherMap(modules: List[Module]): Map[Class[_], Class[_]] = modules.flatMap(_ match {
    case c:Publisher =>
      val keys = c.asInstanceOf[Publisher].publish(createReportDependencies()).asInstanceOf[Depends].dependsOn
      keys.map(_ -> c.iKey())
    case _ => List.empty
  }).toMap


  /**
    * order list of the modules by dependencies if X depends on Y then X is before Y
    *
    * @param in input modules
    * @param map map between interface and Module, given mapping is defined by `Publisher`
    * @param set helper set contains included dependencies (out)
    * @param out dependencies ordered by
    * @return ordered list of modules
    */
  private def order(in: List[Module], map:Map[Class[_], Class[_]], set: Set[Class[_]] = Set.empty, out: List[Module] = Nil): List[Module] = {

    // one round of this method sohould put at least one elemnt to set and out
    def orderRound(in: List[Module], set: Set[Class[_]], out: List[Module] = Nil): (Set[Class[_]], List[Module]) = {
      val des = createDependencies(set, map)
      in match {
        case x :: xs => {
          val r = x.dependencies(des)
          if (r.res) {
            orderRound(xs, set + x.iKey(), x :: out)
          } else {
            orderRound(xs, set, out)
          }
        }
        case _ => (set, out)
      }
    }

    val (rset, rout) = orderRound(in, set)
    if (in.isEmpty) {
      rout ++ out
    } else {
      if (rout.isEmpty) {
        val missingMap = in.reverse.flatMap{m=>
          val dependsOn = m.dependencies(createReportDependencies()).asInstanceOf[Depends].dependsOn
          // missing dependencies for given module
          val moduleDiffs = dependsOn.map{ dependency =>
            // try get transform to modules
            map.getOrElse(dependency, dependency)
          }.toSet.diff(in.map(_.iKey()).toSet).map(_.getSimpleName)
          if(moduleDiffs.isEmpty) List.empty else List((m.toString, moduleDiffs))
        }
        val mappingMsg = missingMap.map{case (k,v) => s"for: $k is missing: ${v.mkString(", ")}"}.mkString("\n")
        val msg =
          s"""
             |Can't initialize modules: (${in.mkString(", ")}), cycle or unresolved dependency detected.
             |The map of missing dependencies:
             |$mappingMsg
             |""".stripMargin
        throw InitializationError(msg)
      }
      val df = in.diff(rout)
      order(df, map,rset, rout ++ out)
    }
  }

  private def init(in: List[Module], out: List[(Module, Throwable)] = Nil)
                  (ctx: Context): (Context, List[(Module, Throwable)]) = in match {
    case x :: xs =>
      if (x.isInstanceOf[Initializable]) {
        log(s"Initialising module: $x")
        Try(x.asInstanceOf[Initializable].initialize(ctx).asTry()).flatten match {
          case Failure(th) => init(xs, (x, th) :: out)(ctx)
          case Success(c) => {
            if(!CTX.isLast(c)) {
              val info = CTX.getInvocationInfo.getOrElse("unknown location")
              log(s"-----------------\nUnused context created at: ${info} during initialization\n-----------------\n")
              if(Akkamo.isContextStrict) {
                throw InitializationError(s"Unused context created: ${info}")
              }
            }
            init(xs, out)(c)
          }
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
        Try(x.asInstanceOf[Runnable].run(ctx).asTry()).flatten match {
          case Failure(th) => run(xs, (x, th) :: out)(ctx)
          case Success(c) => {
            if(!CTX.isLast(c)) {
              val info = CTX.getInvocationInfo.getOrElse("unknown location")
              log(s"-----------------\nUnused context created at: ${info} during run\n-----------------\n")
              if(Akkamo.isContextStrict) {
                throw RunError(s"Unused context created: ${info}")
              }
            }
            run(xs, out)(c)
          }
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
        case (e, (_, th)) => {
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
        log(s"Dispose module: ${x}")
        Try(x.asInstanceOf[Disposable].dispose(ctx).asTry()) match {
          case Failure(th) => dispose(xs, (x, th) :: out)(ctx)
          case _ => dispose(xs, out)(ctx)
        }
      } else {
        dispose(xs, out)(ctx)
      }
    case _ => out
  }

  private def createDependencies(dependencies: Set[Class[_]], map:Map[Class[_], Class[_]]): TypeInfoChain = {
    class W(val res: Boolean) extends TypeInfoChain {
      override def &&[K](implicit ct: ClassTag[K]): TypeInfoChain = {
        val mr = map.get(ct.runtimeClass)
        val mappedRes = mr.map(p=>dependencies.contains(p))
        new W(this.res && (dependencies.contains(ct.runtimeClass) || mappedRes.getOrElse(false)))
      }
    }
    new W(true)
  }

  private trait Depends {
    def dependsOn:Seq[Class[_]]
  }

  private def createReportDependencies() = {
    class W(val dependsOn:Seq[Class[_]], val res:Boolean = false) extends TypeInfoChain with Depends {
      override def &&[T](implicit ct: ClassTag[T]): TypeInfoChain = {
        new W(ct.runtimeClass +: this.dependsOn)
      }
    }
    new W(Nil)
  }
}

object Akkamo {

  import scala.collection.JavaConverters._

  def modules() = java.util.ServiceLoader.load[Module](classOf[Module]).asScala.toList

  /**
    * if the System property named `Strict` is defined,
    * then system throws an Exception if last created Context is not returned from [[eu.akkamo.Initializable#initialize]] or [[eu.akkamo.Runnable#run]]
    *
    */
  val Strict = "akkamo.ctx.strict"

  def isContextStrict = System.getProperty(Strict, "true").toBoolean

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
case class InitializationError(message: String, cause: Throwable = null) extends AkkamoError(message, cause)

/**
  * Error thrown when Akkamo run phase fails (e.g. one of the ran modules throws
  * [[eu.akkamo.RunnableError]]).
  *
  * @param message detail message
  * @param cause   optional error cause
  */
case class RunError(message: String, cause: Throwable = null) extends AkkamoError(message, cause)

/**
  * Error thrown when Akkamo dispose phase fails (e.g. one of the disposed modules throws
  * [[eu.akkamo.DisposableError]]).
  *
  * @param message detail message
  * @param cause   optional error cause
  */
case class DisposeError(message: String, cause: Throwable = null) extends AkkamoError(message, cause)
