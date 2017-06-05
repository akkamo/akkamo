package eu.akkamo.web

import java.io.File

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import eu.akkamo.web.WebContentRegistry.RouteGenerator


/**
  * Simple route generator. Provided Route returns content loaded from path. <br/>
  * Path is defined as `base` + remaining part
  * @author jubu
  */
class FileFromDirGenerator(val base: Either[File, String]) extends RouteGenerator {

  /**
    * Returned `Route` instance handle content of remaining part as b
    *
    * @return route instance
    */
  override def apply(): Route = extractUnmatchedPath(remaining => apply(remaining.toString()))

  def apply(suffix: String) = base match {
    case Left(dir) => fromFile(new File(dir, suffix))
    case Right(url) => fromResource(url + suffix)
  }

  def fromFile(f: File) = getFromFile(f)

  def fromResource(path: String) = getFromResource(path)
}