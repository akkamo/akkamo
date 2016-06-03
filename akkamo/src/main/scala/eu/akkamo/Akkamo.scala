package eu.akkamo

import scala.util.{Failure, Success, Try}

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

	override def register[T <: AnyRef](value: T, key: Option[String])(implicit ct: ClassTag[T]): Unit = {
		val key2Inst = class2Key2Inst.getOrElse(ct.runtimeClass, mutable.Map.empty)
		val realKey = key.getOrElse(Default)
		if (key2Inst.contains(realKey)) {
			throw InitializationError(s"module: $value under key: $key already registered")
		}
		key2Inst += (key.getOrElse(Default) -> value)
		class2Key2Inst += (ct.runtimeClass -> key2Inst)
	}


	def createDependee(dependencies:Set[Class[_]]): Dependency = {
		new Dependency {
			override def &&[T <: Module with Initializable](implicit ct: ClassTag[T]): Dependency = {
				case class W(last: Boolean) extends Dependency {
					override def &&[K <: Module with Initializable](implicit ct: ClassTag[K]): Dependency =
						W(last && dependencies.contains(ct.runtimeClass))
					override def res: Boolean = last
				}
				W(this.res)
			}
			override def res: Boolean = true
		}
	}
}


class AkkamoRun extends ((CTX) => List[Module]) {

	import Logger._

	import scala.collection.JavaConversions._

	type IModule = Module with Initializable

	override def apply(ctx: CTX): List[Module] = {

		def orderRound(in: List[IModule], set:Set[Class[_]], out: List[IModule] = Nil): (Set[Class[_]], List[IModule])  =  {
			val des = ctx.createDependee(set)
			in match {
				case x::xs => {
					val r = x.dependencies(des)
					if(r()) {
						orderRound(xs, set + x.iKey, x::out)
					} else {
						orderRound(xs, set, out)
					}
				}
				case _ => (set, out)
			}
		}

		def order(in: List[IModule], set:Set[Class[_]] = Set.empty, out: List[IModule] = Nil):(Set[Class[_]], List[IModule]) = {
			val ret = orderRound(in, set)
			if(in.isEmpty) {
				(ret._1, ret._2 ++ out )
			} else {
				if(ret._2.isEmpty) {
					throw InitializationError(s"Can't initialize modules: $in, cycle or unresolved dependency detected.")
				}
				order(in.diff(ret._2), ret._1, ret._2 ++ out)
			}
		}

		def init(in:List[IModule], out:List[(IModule, Throwable)] = Nil):List[(IModule, Throwable)] = in match {
			case x :: xs => Try(x.initialize(ctx)) match {
				case Failure(th) => init(xs, (x, th)::out)
				case _ => {
					log(s"Module: $x installed")
					init(xs, out)
				}
			}
			case _ => out
		}

		val modules = java.util.ServiceLoader.load[Module](classOf[Module]).toList
		log(s"Installing modules: $modules")

		// at least one module is initializable and one none
		val grouped: Map[Boolean, List[Module]] = modules.groupBy {
			case x: Initializable => true
			case _ => false
		}

		val (set, ordered)= order(grouped.get(true).get.map(_.asInstanceOf[IModule]))

		log(s"Initializing modules: ${modules.reverse}")

		val errors = init(ordered.reverse)
		// end of game
		if (!errors.isEmpty) {
			val e = InitializationError(s"Somme errors occurred during initialization")
			errors.foldLeft(e) {
				case (e, (m, th)) => {
					e.addSuppressed(th);
					e
				}
			}

			// dispose all
			Try(new AkkamoDispose()(ctx, modules))
			throw e
		}

		val all = grouped.get(false).getOrElse(Nil):::ordered

		// run modules
		val notRunning = all.filter {
			case p: Runnable => true
			case _ => false
		}.map(_.asInstanceOf[Runnable with Module]).map { p =>
			(p, Try {
				p.run(ctx);
				p
			})
		}.filter(!_._2.isSuccess).map { case (p, thp) =>
			(p, thp match {
				case Failure(th) => th;
				case _ => new Error()
			})
		}

		if (!notRunning.isEmpty) {
			Try(new AkkamoDispose()(ctx, modules))
			throw notRunning.foldLeft(RunError(s"Some errors occurred during attempt to run installed modules")) { case (e, (m, th)) =>
				e.addSuppressed(th)
				e
			}
		}
		ctx.inject[LoggingAdapterFactory].map(_.apply(this).info("All modules has been installed"))
		all // return all initialised modules
	}
}

class AkkamoDispose extends ((CTX, List[Module]) => Unit) {

	import Logger._

	override def apply(ctx: CTX, modules: List[Module]): Unit = modules.map {
		case p: Disposable => {
			log(s"Executing dispose on: $p")
			Try(p.dispose(ctx))
		}
		case _ =>
	}

}

/**
	* @author jubu
	*/
object Akkamo extends App {

	import Logger._

	val akkamoRun = new AkkamoRun
	val akkamoDispose = new AkkamoDispose

	val ctx: CTX = new CTX
	Try(akkamoRun(ctx)) match {
		case Failure(th) => {
			log(s"Can't initialize application, reason: ${th.getMessage}!", true)
			th.printStackTrace(Console.err)
			log("System exit", true)
			sys.exit(-1)
		}
		case Success(modules) => {
			Runtime.getRuntime.addShutdownHook(new Thread() {
				override def run() = {
					akkamoDispose(ctx, modules)
				}
			})
		}
	}
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

