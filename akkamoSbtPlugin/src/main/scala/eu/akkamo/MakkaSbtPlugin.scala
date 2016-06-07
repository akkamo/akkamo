package eu.akkamo

import java.net.URLClassLoader
import java.util.concurrent.atomic.AtomicReference

import sbt._
import sbt.Keys._
import sbt.classpath.NullLoader

import scala.pickling.runtime
import scala.util.Try

/**
	* Plugin running akkamo
	*
	* @author jubu
	*/
object AkkamoSbtPlugin extends AutoPlugin {

	val Verbose = "akkamo.verbose"

	case class AkkamoState(data: Option[(Any, Any)] = None,
	                       classLoader: URLClassLoader, project:ProjectRef, initialVerbose:Boolean = initVerbose)

	private[this] val akkamoState = new AtomicReference(AkkamoState(classLoader = null, project = null))

	object autoImport {
		val runAkkamo = TaskKey[Unit]("run-akkamo", "run akkamo application")
		val stopAkkamo = TaskKey[Unit]("stop-akkamo", "stop akkamo application")
	}

	import autoImport._



	override def globalSettings = Seq(
		mainClass in Compile := Some("eu.akkamo.Akkamo")
	) ++  super.globalSettings

	override def projectSettings = Seq(
		runAkkamo  <<= (thisProjectRef, fullClasspath in runAkkamo in Runtime).map(handleStartAkkamo).dependsOn(products in Runtime),
		stopAkkamo <<= (thisProjectRef, fullClasspath in stopAkkamo in Runtime).map(handleStopAkkamo).dependsOn(products in Runtime)
	)

	def handleStartAkkamo(project: ProjectRef, cp:Classpath) = {
		val urls = cp.map(_.data.toURI.toURL).toArray
		val parent = ClassLoader.getSystemClassLoader.getParent
		val classLoader = new URLClassLoader(urls,parent)
		start(stop(setVerbose(akkamoState.get())).copy(classLoader = classLoader), cleanVerbose)
	}

	def handleStopAkkamo(project: ProjectRef, cp:Classpath) = {
		val urls = cp.map(_.data.toURI.toURL).toArray
		val parent = ClassLoader.getSystemClassLoader.getParent
		val classLoader = new URLClassLoader(urls,parent)
		akkamoState.set(cleanVerbose(stop(setVerbose(akkamoState.get()))))
	}


	private val start = (state: AkkamoState, fn:(AkkamoState)=>AkkamoState) => {
		val runnable = new Runnable {
			override def run(): Unit = {
				val ctxClass = Class.forName("eu.akkamo.CTX", true, Thread.currentThread().getContextClassLoader)
				val ctx = ctxClass.newInstance().asInstanceOf[Object]
				val akkamoRun = Class.forName(
					"eu.akkamo.AkkamoRun", true, Thread.currentThread().getContextClassLoader).newInstance()
				val data = akkamoRun.getClass.getDeclaredMethods.find{p=>
					val types = p.getParameterTypes
					p.getName == "apply" && types.length == 1 && ctxClass.isAssignableFrom(types(0))
				}.map { m =>
					m.invoke(akkamoRun,ctx)
				}.get
				akkamoState.set(fn(state.copy(Some((ctx, data)))))
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

	private val stop = (state: AkkamoState) => {
		val thread = state.data.map{case (ctx, data) =>
			val runnable = new Runnable {
				override def run(): Unit = {
					val akkamoDispose = Class.forName(
						"eu.akkamo.AkkamoDispose", true, Thread.currentThread().getContextClassLoader).newInstance()
					akkamoDispose.getClass.getDeclaredMethods.find{p=>
						val types = p.getParameterTypes
						p.getName == "apply" && types.length == 2 && ctx.getClass.isAssignableFrom(types(0))
					}.map { m =>
						m.invoke(akkamoDispose,ctx.asInstanceOf[Object], data.asInstanceOf[Object])
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

	private def initVerbose = System.getProperty(Verbose) != null

	private def setVerbose(state: AkkamoState) = {
		if(!state.initialVerbose) {
			// we want have verbose start by default
			if (System.getProperty(Verbose) == null) {
				System.setProperty(Verbose, true.toString)
			}
		}
		state
	}

	private def cleanVerbose(state: AkkamoState) = {
		if(!state.initialVerbose) 	{
			System.clearProperty(Verbose)
		}
		state
	}
}