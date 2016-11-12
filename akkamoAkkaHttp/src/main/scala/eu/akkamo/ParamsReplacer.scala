package eu.akkamo

import akka.event.LoggingAdapter

import scala.util.matching.Regex

/**
  * Provides bunch of functions for replacing parameters in input string with real values. Used
  * syntax for parameters is selected to be like `%{PARAM_NAME}`, in order not to interfere with
  * common used syntax, e.g. for string interpolation in Scala.
  *
  * @author Vaclav Svejcar (vaclav.svejcar@gmail.com)
  */
private[akkamo] object ParamsReplacer {

  val paramsRegex: Regex = """\%\{(.*?)\}""".r

  /**
    * Replace all parameters in source string by real values, defined in the `replacements` map.
    * Parameters must be defined using the syntax `${PARAM_NAME}`. Keys in the `replacements` map
    * represents the parameter names (just the plain name, e.g. `PARAM_NAME`, without braces and
    * percent sign), and values are the actual replacement values. If optional instance of
    * `LoggingAdapter` is provided, all parameters for which the replacement has not been found
    * will be logged as a warning message.
    *
    * @param source       source string where the parameters will be replaced
    * @param replacements map of replacements (param name -> actual value)
    * @param log          optional logger for logging all parameters for which replacements were not
    *                     found
    * @return string where parameters are replaced with real values
    */
  def replaceParams(source: String, replacements: Map[String, String],
                    log: Option[LoggingAdapter] = None): String = {

    def replace: Regex.Match => String = {
      case Regex.Groups(index) => replacements.getOrElse(index, {
        log.foreach(_.warning(s"Invalid param '${index}' in source string '${source}'"))
        s"%{$index}"
      })
    }

    paramsRegex.replaceAllIn(source, replace)
  }

}
