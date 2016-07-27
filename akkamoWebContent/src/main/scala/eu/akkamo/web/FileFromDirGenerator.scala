package eu.akkamo.web

import java.io.File

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.RouteDirectives.{complete => _}
import eu.akkamo.web.WebContentRegistry.RouteGenerator

/**
  * @author jubu
  */
class FileFromDirGenerator(source: File) extends RouteGenerator {

  def this() = this(new File(FileFromDirGenerator.defaultUri))

  override def apply(): Route = extractUnmatchedPath { remaining =>
    getFromFile(new File(source, remaining.toString()))
  }
}


object FileFromDirGenerator {

  val Prefix = "web"

  def defaultUri = nameToDirUri(Prefix)

  @throws[IllegalArgumentException]
  def nameToDirUri(path: String) = {

    def fileIsOk(f:File) = f.exists() && f.isDirectory

    val df = new File(path)
    if(fileIsOk(df)) df.toURI else {
      // build in zip
      val res = this.getClass.getClassLoader.getResource(path)
      if (res != null) res.toURI
      else {
        // user dir
        val d = System.getProperty("user.dir") + File.separator + path
        val df = new File(d)
        if (fileIsOk(df)) {
          df.toURI
        } else {
          // user home
          val d = System.getProperty("user.home") + File.separator + path
          val df = new File(d)
          if (fileIsOk(df)) {
            df.toURI
          } else throw new IllegalArgumentException(s"Path: $path doesn't exists")
        }
      }
    }
  }
}