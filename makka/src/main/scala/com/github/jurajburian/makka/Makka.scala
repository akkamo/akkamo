package com.github.jurajburian.makka


import scala.util.{Failure, Success, Try}


class CTX extends Context {

	import scala.collection._
	import scala.reflect.ClassTag

	val Default = "$DEFAULT$"
	val class2Key2Inst = mutable.Map.empty[Class[_], mutable.Map[String, AnyRef]]
	val runningSet = mutable.Set.empty[Class[_]]
	val initializedSet = mutable.Set.empty[Class[_]]

	override def inject[T](implicit ct: ClassTag[T]): Option[T] = {
		inject(Default)
	}

	override def inject[T](key: String, strict:Boolean = false)(implicit ct: ClassTag[T]): Option[T] = {
		class2Key2Inst.get(ct.runtimeClass).flatMap { p =>
			val ret = p.get(key)
			if(ret.isEmpty && !strict) {
				p.get(Default)
			} else {
				ret
			}
		}.map(_.asInstanceOf[T])
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

	override def initialized[T <: Module with Initializable](implicit ct: ClassTag[T]): Boolean = {
		val ret = initializedSet.contains(ct.runtimeClass)
		ret
	}

	override def initializedWith[T <: Module with Initializable](implicit ct: ClassTag[T]): With = {
		case class W(last:Boolean) extends With {
			override def &&[K <: Module with Initializable](implicit ct: ClassTag[K]):With =
				W(initializedSet.contains(ct.runtimeClass))
			override def res: Boolean = last
		}
		W(initializedSet.contains(ct.runtimeClass))
	}

	override def running[T <: Module with Runnable](implicit ct: ClassTag[T]): Boolean = {
		val ret = runningSet.contains(ct.runtimeClass)
		ret
	}

	override def runningWith[T <: Module with Initializable](implicit ct: ClassTag[T]): With = {
		case class W(last:Boolean) extends With {
			override def &&[K <: Module with Initializable](implicit ct: ClassTag[K]):With =
				W(runningSet.contains(ct.runtimeClass))
			override def res: Boolean = last
		}
		W(runningSet.contains(ct.runtimeClass))
	}


	private[makka] def addInitialized[T <: Module with Initializable](p: T) = {
		initializedSet += p.iKey()
	}

	private[makka] def addRunning[T <: Module with Runnable](p: T) = {
		runningSet += p.rKey()
	}
}


class MakkaRun extends ((CTX) => List[Module]) {

	import scala.collection.JavaConversions._

	type IModule = Module with Initializable

	override def apply(ctx: CTX): List[Module] = {

		type RES = (List[IModule], List[Module], List[(IModule, Throwable)])

		/*
			* @param input imodules to be initialised
			* @return pair of not initialized, initialized modules together with the list of pairs error, module
			*/
		def initRound(input: List[IModule], out: RES): RES = input match {
			case x :: xs => Try(x.initialize(ctx)) match {
				case Success(isInitialized) =>
					if (!isInitialized) {
						initRound(xs, out.copy(_1 = x :: out._1))
					} else {
						ctx.addInitialized(x)
						initRound(xs, out.copy(_2 = x :: out._2))
					}
				case Failure(th) => initRound(xs, out.copy(_3 = (x, th) :: out._3))
			}
			case _ => out
		}

		def init(input: List[IModule], out: RES): RES = {
			val res = initRound(input, out)
			if (res._1.isEmpty) {
				res
			} else if (input.size <= res._1.size) {
				res
			} else {
				init(res._1, res.copy(_1 = List.empty))
			}
		}

		val modules = java.util.ServiceLoader.load[Module](classOf[Module]).iterator().toList
		println(s"Installing modules: $modules")

		// at leas one module is initializable and one none
		val grouped: Map[Boolean, List[Module]] = modules.groupBy {
			case x: Initializable => true;
			case _ => false
		}

		// init modules
		val (nis, is, ths) = init(grouped.get(true).get.map(_.asInstanceOf[IModule]),
			(List.empty[IModule], grouped.get(false).getOrElse(List.empty), List.empty[(IModule, Throwable)]))

		// throw exception if cycle detected or there is some non empty set of exceptions
		if (!(nis.isEmpty && ths.isEmpty)) {
			// also fill old causes
			val e = if (!ths.isEmpty) {
				InitializationError(s"Somme errors occurred during initialization")
			} else {
				InitializationError(s"Can't initialize modules: $nis, cycle or unresolved dependency")
			}
			ths.foldLeft(e) {
				case (e, (m, th)) => {
					e.addSuppressed(th);
					e
				}
			}
			// dispose all
			Try(new MakkaDispose()(ctx, modules))
			throw e
		}

		// run modules
		val notRunning = is.filter {
			case p: Runnable => true
			case _ => false
		}.map(_.asInstanceOf[Runnable with Module]).map { p =>
			(p, Try {
				p.run(ctx);
				ctx.addRunning(p);
				p
			})
		}.filter(!_._2.isSuccess).map { case (p, thp) =>
			(p, thp match {
				case Failure(th) => th;
				case _ => new Error()
			})
		}

		if (!notRunning.isEmpty) {
			Try(new MakkaDispose()(ctx, modules))
			throw notRunning.foldLeft(RunError(s"Somme errors occurred during attempt to run installed modules")) { case (e, (m, th)) =>
				e.addSuppressed(th)
				e
			}
		}
		ctx.inject[LoggingAdapterFactory].map(_.apply(this).info("All modules has been installed"))
		is // return all initialised modules
	}
}

class MakkaDispose extends ((CTX, List[Module]) => Unit) {

	override def apply(ctx: CTX, modules: List[Module]): Unit = modules.map {
		case p: Disposable => {
			println(s"Executing dispose on: $p")
			Try(p.dispose(ctx))
		}
		case _ =>
	}
}


/**
	* @author jubu
	*/
object Makka extends App {
	val makkaRun = new MakkaRun
	val makkaDispose = new MakkaDispose

	val ctx: CTX = new CTX
	Try(makkaRun(ctx)) match {
		case Failure(th) => {
			Console.err.println(s"Can't initialize application, reason: ${th.getMessage}!")
			th.printStackTrace(Console.err)
			Console.err.println("System exit")
			sys.exit(-1)
		}
		case Success(modules) => {
			Runtime.getRuntime.addShutdownHook(new Thread() {
				override def run() = {
					makkaDispose(ctx, modules)
				}
			})
		}
	}
}

case class InitializationError(message: String, cause: Throwable = null) extends Error(message, cause)

case class RunError(message: String, cause: Throwable = null) extends Error(message, cause)
