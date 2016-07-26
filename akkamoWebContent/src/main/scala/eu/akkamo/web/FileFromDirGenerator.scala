package eu.akkamo.web

import java.io.File

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.directives.RouteDirectives.{complete => _}
import akka.http.scaladsl.server.{RequestContext, Route}
import eu.akkamo.web.WebContentRegistry.RouteGenerator

/**
  * @author jubu
  */
class FileFromDirGenerator(path: String, resource: Boolean) extends RouteGenerator {
  def this() = this("/web", true)

  def this(path: String, resource: String) = this(path, (resource.trim.toLowerCase.equals("true")))

  override def apply(ctx: RequestContext): Route = extractUnmatchedPath { remaining =>
    val sep = File.pathSeparator
    val src = s"$path$sep$remaining"
    if (resource) getFromResource(src) else getFromFile(src)
  }

  def stream(path: String) = {
    this.getClass.getClassLoader.getResourceAsStream(path)
  }
}
