package com.github.jurajburian.makka

import sun.invoke.empty.Empty

import scala.reflect.api.Types
import scala.util.{Failure, Try}

class Main {

	import scala.collection.JavaConversions._
	val modules= java.util.ServiceLoader.load[Module](classOf[Module]).iterator().toList

	val ctx = new Context {

		import scala.reflect.runtime.universe.TypeTag
		import scala.collection._

		val Default = "$DEFAULT$"
		val tpe2Key2Inst = mutable.Map.empty[Types#Type, mutable.Map[String, AnyRef]]

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
		override def register[T<:AnyRef](value: T, key: Option[String])(implicit tt: TypeTag[T]): Unit = {
			val tpe = tt.tpe
			val key2Inst = tpe2Key2Inst.getOrElse(tpe, mutable.Map.empty)
			val realKey = key.getOrElse(Default)
			if(key2Inst.contains(realKey)) {
				throw InitializationError(s"module: $value under key: $key already registered")
			}
			key2Inst += (key.getOrElse(Default)->value)
			tpe2Key2Inst += (tpe->key2Inst)
		}
	}

	def run() = {
		println(s"Installing modules: $modules")
		// init modules
		init(modules)
		// run modules
		modules.map {case p:Startable=> p.start(ctx); case _ =>}
		ctx.inject[LoggingAdapterFactory].map(_.apply(this).info("All modules has been installed"))
	}


	/**
		*
		* @param modules - input modules
		* @return remaining, not initialized modules
		*/
	protected def init(modules:List[Module]):List[Module] = {
		val ret = modules.filter{
			case p:Initializable=> !p.initialize(ctx)
			case _=> false
		}
		if(ret.size >= modules.size) {
			throw InitializationError(s"Can't initialize modules: $ret, cycle or unresolved dependency")
		}
		if(ret.isEmpty) {
			ret
		} else {
			init(ret)
		}
	}
}


/**
	* @author jubu
	*/
object Main extends App {
	val main = new Main()
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
				println("Shutdown...")  // TODO modules shutdown to be here
			}
		})
	}
}

case class InitializationError(message: String, cause: Throwable = null) extends Error(message, cause)
