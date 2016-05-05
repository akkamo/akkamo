package com.github.jurajburian.makka
import java.net.URLClassLoader
import java.util.concurrent.atomic.AtomicReference

import sbt._
import sbt.Keys._
import sbt.classpath.NullLoader

import scala.util.Try

/**
	* Plugin running makka
	* @author jubu
	*/
object MakkaSbtPlugin extends AutoPlugin {

	override def trigger = allRequirements

	case class MakkaState(data: Option[(Any, Any)] = None, classLoader: URLClassLoader, project:ProjectRef)

	private[this] val makkaState = new AtomicReference(MakkaState(classLoader = null, project = null))

	object autoImport {
		val runMakka = TaskKey[Unit]("run-makka", "run makka application")
	}

	import autoImport._

	override def projectSettings = Seq(
		fullClasspath in runMakka <<= fullClasspath in Runtime,
		runMakka <<= (thisProjectRef, fullClasspath in runMakka).map(handleMakka).dependsOn(products in Compile)
	)

	def handleMakka(project: ProjectRef, cp:Classpath) = {
		val urls = cp.map(_.data.toURI.toURL).toArray
		val parent = ClassLoader.getSystemClassLoader.getParent
		val classLoader = new URLClassLoader(urls,parent)
		start(stop(makkaState.get()).copy(classLoader = classLoader))
	}

	private val start = (state: MakkaState) => {
		val runnable = new Runnable {
			override def run(): Unit = {
				val ctxClass = Class.forName("com.github.jurajburian.makka.CTX", true, Thread.currentThread().getContextClassLoader)
				val ctx = ctxClass.newInstance().asInstanceOf[Object]
				val makkaRun = Class.forName(
					"com.github.jurajburian.makka.MakkaRun", true, Thread.currentThread().getContextClassLoader).newInstance()
				val data = makkaRun.getClass.getDeclaredMethods.find{p=>
					val types = p.getParameterTypes
					p.getName == "apply" && types.length == 1 && ctxClass.isAssignableFrom(types(0))
				}.map { m =>
					m.invoke(makkaRun,ctx)
				}.get
				makkaState.set(state.copy(Some((ctx, data))))
			}
		}
		val thread = new Thread(runnable)
		thread.setContextClassLoader(state.classLoader)
		thread.setDaemon(true)
		thread.start
	}

	private val stop = (state: MakkaState) => {
		val thread = state.data.map{case (ctx, data) =>
			val runnable = new Runnable {
				override def run(): Unit = {
					val makkaDispose = Class.forName(
						"com.github.jurajburian.makka.MakkaDispose", true, Thread.currentThread().getContextClassLoader).newInstance()
					makkaDispose.getClass.getDeclaredMethods.find{p=>
						val types = p.getParameterTypes
						p.getName == "apply" && types.length == 2 && ctx.getClass.isAssignableFrom(types(0))
					}.map { m =>
						m.invoke(makkaDispose,ctx.asInstanceOf[Object], data.asInstanceOf[Object])
					}
				}
			}
			val thread = new Thread(runnable)
			thread.setContextClassLoader(state.classLoader)
			thread.setDaemon(true)
			thread.start
			thread
		}
		thread.map{th=>
			while(th.getState != Thread.State.TERMINATED) {
				Try(Thread.sleep(100))
			}
		}
		state.copy(data = None, classLoader =  null, project = null)
	}
}