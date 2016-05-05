package com.github.jurajburian.makka


import scala.util.{Failure, Success, Try}


class CTX extends Context {

	import scala.collection._
	import scala.reflect.ClassTag

	val Default = "$DEFAULT$"
	val class2Key2Inst = mutable.Map.empty[Class[_], mutable.Map[String, AnyRef]]
	val runningSet = mutable.Set.empty[Class[_]]
	val initializedSet = mutable.Set.empty[Class[_]]


	/**
		* inject service
		*
		* @param ct
		* @tparam T require
		* @return implementation of interface `T`
		*/

	override def inject[T](implicit ct: ClassTag[T]): Option[T] = {
		inject(Default)
	}

	/**
		* inject service
		*
		* @param key additional mapping identifier
		* @tparam T
		* @return
		*/
	override def inject[T](key: String)(implicit ct: ClassTag[T]): Option[T] = {
		class2Key2Inst.get(ct.runtimeClass).flatMap(_.get(key)).map(_.asInstanceOf[T])
	}

	/**
		*
		* @param value
		* @param key
		* @param ct
		* @tparam T
		*/
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

	override def running[T <: Module with Runnable](implicit ct: ClassTag[T]): Boolean = {
		val ret = runningSet.contains(ct.runtimeClass)
		ret
	}

	private[makka] def addInitialized[T <: Module with Initializable](p: T) = {
		initializedSet += p.getClass
	}

	private[makka] def addRunning[T <: Module with Runnable](p: T)(implicit ct: ClassTag[T]) = {
		runningSet += p.getClass
	}
}


class MakkaRun extends ((CTX)=>List[Module]) {

	import scala.collection.JavaConversions._

	override def apply(ctx:CTX) = {

		def init(modules: List[Module]): List[Module] = {
			var is = List.empty[Module]
			val ret = modules.filter {
				case p: Initializable => {
					val isInitialized = p.initialize(ctx)
					if (isInitialized) {
						ctx.addInitialized(p)
						is = p :: is
					}
					!isInitialized
				}
				case _ => false
			}
			if (ret.size >= modules.size) {
				throw InitializationError(s"Can't initialize modules: $ret, cycle or unresolved dependency")
			}
			if (ret.isEmpty) {
				ret
			} else {
				is ++ init(ret)
			}
		}

		val modules = java.util.ServiceLoader.load[Module](classOf[Module]).iterator().toList
		println(s"Installing modules: $modules")
		// init modules
		val initializedModules = init(modules).reverse
		// run modules
		val ret = (modules.diff(initializedModules) ++ initializedModules)
		ret.map {
			case p: Runnable => {
				p.run(ctx)
				ctx.addRunning(p)
			}
			case _ =>
		}
		ctx.inject[LoggingAdapterFactory].map(_.apply(this).info("All modules has been installed"))
		ret
	}
}

class MakkaDispose extends ((CTX, List[Module])=>Unit) {

	override def apply(ctx:CTX, modules:List[Module]):Unit = modules.map{
			case p:Disposable => {
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

	val ctx:CTX = new CTX
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
