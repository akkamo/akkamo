package com.github.jurajburian.makka
import java.net.URLClassLoader
import java.util.concurrent.atomic.AtomicReference

import sbt._
import sbt.Keys._
import sbt.classpath.NullLoader

import scala.util.Try

/**
	* Plugin running makka
	*
	* @author jubu
	*/
object MakkaSbtPlugin extends AutoPlugin {

	{
		// we want have verbose start by default
		val v = "makka.verbose"
		if (System.getProperty(v) == null) {
			System.setProperty(v, true.toString)
		}
	}

	override def trigger = allRequirements

	case class MakkaState(data: Option[(Any, Any)] = None, classLoader: URLClassLoader, project:ProjectRef)

	private[this] val makkaState = new AtomicReference(MakkaState(classLoader = null, project = null))

	object autoImport {
		val runMakka = TaskKey[Unit]("run-makka", "run makka application")
		val stopMakka = TaskKey[Unit]("stop-makka", "stop makka application")
	}

	import autoImport._

	override def projectSettings = Seq(
		mainClass in Global := Some("com.github.jurajburian.makka.Makka"),
		fullClasspath in runMakka <<= fullClasspath in Runtime,
		runMakka <<= (thisProjectRef, fullClasspath in runMakka).map(handleStartMakka).dependsOn(products in Compile),
		stopMakka <<= (thisProjectRef, fullClasspath in runMakka).map(handleStopMakka)
	)

	def handleStartMakka(project: ProjectRef, cp:Classpath) = {
		val urls = cp.map(_.data.toURI.toURL).toArray
		val parent = ClassLoader.getSystemClassLoader.getParent
		val classLoader = new URLClassLoader(urls,parent)
		start(stop(makkaState.get()).copy(classLoader = classLoader))
	}

	def handleStopMakka(project: ProjectRef, cp:Classpath) = {
		val urls = cp.map(_.data.toURI.toURL).toArray
		val parent = ClassLoader.getSystemClassLoader.getParent
		val classLoader = new URLClassLoader(urls,parent)
		makkaState.set(stop(makkaState.get()))
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
		while(thread.getState != Thread.State.TERMINATED) {
			Try(Thread.sleep(100))
		}
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