package eu.akkamo

import akka.event.LoggingAdapter
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.http.scaladsl.server.directives.{DebuggingDirectives, LogEntry, LoggingMagnet}
import akka.http.scaladsl.server.{Directive0, Rejection, RouteResult}

/**
  * Helper functions providing functionality related to logging.
  *
  * @author Vaclav Svejcar (vaclav.svejcar@gmail.com)
  */
object AkkaHttpLoggingUtil {

  /**
    * Helper function used to log HTTP requests for specific route.
    *
    * = Example of use =
    * {{{
    *   val logged = requestLogger(
    *     completed = (req, res) =>
    *       LogEntry(s"${req.method}:${req.uri}: ${res.status}", Logging.InfoLevel),
    *     rejected =(req, rejections) =>
    *       LogEntry(s"${req.method}:${req.uri}: request was rejected", Logging.ErrorLevel))
    *   )
    *
    * val route = logged {
    *   path("foo") {
    *     get {
    *       complete("Hello, world!")
    *     }
    *   }
    * }
    *
    * // register route as normally into the route registry
    * ctx.registerIn[RouteRegistry, Route](route)
    * }}}
    *
    * @param completed function transforming HTTP request and response into log message in case of
    *                  request success
    * @param rejected  function transforming HTTP request and list of rejections in case that the
    *                  request is rejected
    * @return Akka HTTP directive
    */
  def requestLogger(completed: (HttpRequest, HttpResponse) => LogEntry,
                    rejected: (HttpRequest, Seq[Rejection]) => LogEntry): Directive0 = {

    def logRequest(log: LoggingAdapter)(req: HttpRequest)(res: RouteResult): Unit = {
      val logEntry: LogEntry = res match {
        case RouteResult.Complete(response) => completed(req, response)
        case RouteResult.Rejected(rejections) => rejected(req, rejections)
      }

      log.log(logEntry.level, logEntry.obj.toString)
    }

    DebuggingDirectives.logRequestResult(LoggingMagnet(logRequest))
  }
}
