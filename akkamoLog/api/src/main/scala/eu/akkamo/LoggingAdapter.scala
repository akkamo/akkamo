package eu.akkamo


object LoggingAdapter {

  type MDC = Map[String, Any]
  val emptyMDC: MDC = Map.empty[String, Any]

  /**
    * Marker trait for annotating LogLevel, which must be Int after erasure.
    */
  case class LogLevel(asInt: Int) {
    
    @inline final def >=(other: LogLevel): Boolean = asInt >= other.asInt

    @inline final def <=(other: LogLevel): Boolean = asInt <= other.asInt

    @inline final def >(other: LogLevel): Boolean = asInt > other.asInt

    @inline final def <(other: LogLevel): Boolean = asInt < other.asInt
  }

  /**
    * Log level in numeric form, used when deciding whether a certain log
    * statement should generate a log event. Predefined levels are ErrorLevel (1)
    * to DebugLevel (4). In case you want to add more levels, loggers need to
    * be subscribed to their event bus channels manually.
    */
  final val ErrorLevel = LogLevel(1)
  final val WarningLevel = LogLevel(2)
  final val InfoLevel = LogLevel(3)
  final val DebugLevel = LogLevel(4)

  /**
    * Internal Akka use only
    *
    * Don't include the OffLevel in the AllLogLevels since we should never subscribe
    * to some kind of OffEvent.
    */
  private final val OffLevel = LogLevel(Int.MinValue)

  /**
    * Returns the LogLevel associated with the given string,
    * valid inputs are upper or lowercase (not mixed) versions of:
    * "error", "warning", "info" and "debug"
    */
  def levelFor(s: String): Option[LogLevel] = s.toLowerCase match {
    case "off" ⇒ Some(OffLevel)
    case "error" ⇒ Some(ErrorLevel)
    case "warning" ⇒ Some(WarningLevel)
    case "info" ⇒ Some(InfoLevel)
    case "debug" ⇒ Some(DebugLevel)
    case unknown ⇒ None
  }
}

/**
  * @author jubu
  */
trait LoggingAdapter{

  import LoggingAdapter._

  def isErrorEnabled : scala.Boolean
  def isWarningEnabled : scala.Boolean
  def isInfoEnabled : scala.Boolean
  def isDebugEnabled : scala.Boolean
  def error(cause : scala.Throwable, message : scala.Predef.String) : Unit 
  def error(cause : scala.Throwable, template : scala.Predef.String, arg1 : scala.Any) : Unit
  def error(cause : scala.Throwable, template : scala.Predef.String, arg1 : scala.Any, arg2 : scala.Any) : Unit
  def error(cause : scala.Throwable, template : scala.Predef.String, arg1 : scala.Any, arg2 : scala.Any, arg3 : scala.Any) : Unit
  def error(cause : scala.Throwable, template : scala.Predef.String, arg1 : scala.Any, arg2 : scala.Any, arg3 : scala.Any, arg4 : scala.Any) : Unit
  def error(message : scala.Predef.String) : Unit
  def error(template : scala.Predef.String, arg1 : scala.Any) : Unit
  def error(template : scala.Predef.String, arg1 : scala.Any, arg2 : scala.Any) : Unit
  def error(template : scala.Predef.String, arg1 : scala.Any, arg2 : scala.Any, arg3 : scala.Any) : Unit
  def error(template : scala.Predef.String, arg1 : scala.Any, arg2 : scala.Any, arg3 : scala.Any, arg4 : scala.Any) : Unit
  def warning(message : scala.Predef.String) : Unit
  def warning(template : scala.Predef.String, arg1 : scala.Any) : Unit
  def warning(template : scala.Predef.String, arg1 : scala.Any, arg2 : scala.Any) : Unit
  def warning(template : scala.Predef.String, arg1 : scala.Any, arg2 : scala.Any, arg3 : scala.Any) : Unit
  def warning(template : scala.Predef.String, arg1 : scala.Any, arg2 : scala.Any, arg3 : scala.Any, arg4 : scala.Any) : Unit
  def info(message : scala.Predef.String) : Unit
  def info(template : scala.Predef.String, arg1 : scala.Any) : Unit
  def info(template : scala.Predef.String, arg1 : scala.Any, arg2 : scala.Any) : Unit
  def info(template : scala.Predef.String, arg1 : scala.Any, arg2 : scala.Any, arg3 : scala.Any) : Unit
  def info(template : scala.Predef.String, arg1 : scala.Any, arg2 : scala.Any, arg3 : scala.Any, arg4 : scala.Any) : Unit
  def debug(message : scala.Predef.String) : Unit
  def debug(template : scala.Predef.String, arg1 : scala.Any) : Unit
  def debug(template : scala.Predef.String, arg1 : scala.Any, arg2 : scala.Any) : Unit
  def debug(template : scala.Predef.String, arg1 : scala.Any, arg2 : scala.Any, arg3 : scala.Any) : Unit
  def debug(template : scala.Predef.String, arg1 : scala.Any, arg2 : scala.Any, arg3 : scala.Any, arg4 : scala.Any) : Unit
  def log(level : LogLevel, message : scala.Predef.String) : Unit
  def log(level : LogLevel, template : scala.Predef.String, arg1 : scala.Any) : Unit
  def log(level : LogLevel, template : scala.Predef.String, arg1 : scala.Any, arg2 : scala.Any) : Unit
  def log(level : LogLevel, template : scala.Predef.String, arg1 : scala.Any, arg2 : scala.Any, arg3 : scala.Any) : Unit
  def log(level : LogLevel, template : scala.Predef.String, arg1 : scala.Any, arg2 : scala.Any, arg3 : scala.Any, arg4 : scala.Any) : Unit

  def isEnabled(level : LogLevel) : scala.Boolean

  def format(t : scala.Predef.String, arg : scala.Any*) : scala.Predef.String
}
