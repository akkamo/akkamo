package com.github.jurajburian.makka
import java.net.URLClassLoader
import java.util.concurrent.atomic.AtomicReference

import sbt._
import sbt.Keys._

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
		println(urls.toSeq)
		val classLoader = new URLClassLoader(urls, getClass.getClassLoader.getParent)
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

/*

		state.data.map { case (ctx, data) =>
			val makkaDisposeClass = Class.forName("com.github.jurajburian.makka.MakkaDispose")
			val di = makkaDisposeClass.newInstance()
			makkaDisposeClass.getMethods.find(_.getName == "apply").map { m =>
				m.invoke(di, Array(ctx, data))
			}
		}
*/
		state.copy(None)
	}
}
