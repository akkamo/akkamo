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
class FileFromDirGenerator(basePath: String) extends RouteGenerator {

  val base = FileFromDirGenerator.toBaseSource(basePath)

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

object FileFromDirGenerator {

  /**
    * find appropriate location of base source et. directory, resource ....
    * @param path
    * @return
    */
  @throws[IllegalArgumentException]
  def toBaseSource(path: String) = {

    def fileIsOk(f: File) = f.exists() && f.isDirectory

    val df = new File(path)
    if (fileIsOk(df)) Left(df)
    else {
      // build in zip
      val res = this.getClass.getClassLoader.getResource(path)
      if (res != null) Right(path)
      else {
        // user dir
        val d = System.getProperty("user.dir") + File.separator + path
        val df = new File(d)
        if (fileIsOk(df)) {
          Left(df)
        } else {
          // user home
          val d = System.getProperty("user.home") + File.separator + path
          val df = new File(d)
          if (fileIsOk(df)) {
            Left(df)
          } else throw new IllegalArgumentException(s"Path: $path doesn't exists")
        }
      }
    }
  }
}