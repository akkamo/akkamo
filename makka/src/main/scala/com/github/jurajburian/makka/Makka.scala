package com.github.jurajburian.makka


import scala.reflect.api.Types
import scala.util.{Failure, Try}

class Makka {

	import scala.collection.JavaConversions._

	private class CTX extends Context {

		import scala.collection._
		import scala.reflect.runtime.universe.TypeTag

		val Default = "$DEFAULT$"
		val tpe2Key2Inst = mutable.Map.empty[Types#Type, mutable.Map[String, AnyRef]]
		val runningSet = mutable.Set.empty[Types#Type]
		val initializedSet = mutable.Set.empty[Types#Type]

		/**
			* inject service
			*
			* @param tt
			* @tparam T require
			* @return implementation of interface `T`
			*/
		override def inject[T](implicit tt: TypeTag[T]): Option[T] = {
			inject(Default)
		}

		/**
			* inject service
			*
			* @param key additional mapping identifier
			* @tparam T
			* @return
			*/
		override def inject[T](key: String)(implicit tt: TypeTag[T]): Option[T] = {
			val tpe = tt.tpe
			tpe2Key2Inst.get(tpe).flatMap(_.get(key)).map(_.asInstanceOf[T])
		}

		/**
			*
			* @param value
			* @param key
			* @param tt
			* @tparam T
			*/
		override def register[T <: AnyRef](value: T, key: Option[String])(implicit tt: TypeTag[T]): Unit = {
			val tpe = tt.tpe
			val key2Inst = tpe2Key2Inst.getOrElse(tpe, mutable.Map.empty)
			val realKey = key.getOrElse(Default)
			if (key2Inst.contains(realKey)) {
				throw InitializationError(s"module: $value under key: $key already registered")
			}
			key2Inst += (key.getOrElse(Default) -> value)
			tpe2Key2Inst += (tpe -> key2Inst)
		}

		override def initialized[T <: Module with Initializable](implicit tt: TypeTag[T]): Boolean = { initializedSet.contains(tt.tpe)}

		override def running[T <: Module with Runnable](implicit tt: TypeTag[T]): Boolean = {runningSet.contains(tt.tpe)}

		private[Makka] def addInitialized[T <: Module with Initializable](p:T)(implicit tt: TypeTag[T])= { initializedSet += tt.tpe}

		private[Makka] def addRunning[T <: Module with Runnable](p:T)(implicit tt: TypeTag[T]) = {runningSet += tt.tpe}
	}

	private val ctx = new CTX

	def run() = {
		val modules = java.util.ServiceLoader.load[Module](classOf[Module]).iterator().toList
		println(s"Installing modules: $modules")
		// init modules
		val initializedModules = init(modules)
		// run modules
		initializedModules.reverse.map { case p: Runnable => {
			p.run(ctx)
			ctx.addRunning(p)
		}; case _ => }
		ctx.inject[LoggingAdapterFactory].map(_.apply(this).info("All modules has been installed"))
	}


	/**
		*
		* @param modules - input modules
		* @return initialized modules
		*/
	protected def init(modules: List[Module]): List[Module] = {
		var is = List.empty[Module]
		val ret = modules.filter {
			case p: Initializable => {
				val isInitialized = p.initialize(ctx)
				if(isInitialized) {
					ctx.addInitialized(p)
					is = p::is
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
}


/**
	* @author jubu
	*/
object Makka extends App {

	val main = new Makka()
	Try(main.run) match {
		case Failure(th) => {
			th.printStackTrace(Console.err)
			System.exit(0)
		}
		case _ => {
			shutdownHook()

		}
	}

	def shutdownHook() = {
		Runtime.getRuntime.addShutdownHook(new Thread() {
			override def run() = {
				println("Shutdown...") // TODO modules shutdown to be here
			}
		})
	}
}

case class InitializationError(message: String, cause: Throwable = null) extends Error(message, cause)
