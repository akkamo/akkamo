package eu.akkamo

import scala.concurrent.{Await, Future, duration}
import scala.util.{Failure, Try}

/**
  * Represents the result holder of [[eu.akkamo.Initializable#initialize]] or
  * [[eu.akkamo.Runnable#run]] operations. The actual operation result is held as an instance of
  * `Try[Context]`.
  *
  * @tparam T type of the result
  */
trait Res[T] {

  /**
    * Returns the operation result.
    *
    * @return operation result
    */
  def asTry(): Try[T]
}


/**
  * Companion object for the class [[Res]], provides implicit conversions from [[Res]] to
  * [[scala.util.Try]] and [[scala.concurrent.Future]].
  */
private[akkamo] object Res {

  implicit def context2res(p:Context) = new Res[Context] {
    override def asTry(): Try[Context] = Try(p)
  }

  implicit def tryContext2res(p: Try[Context]): Res[Context] = new Res[Context] {
    override def asTry(): Try[Context] = p
  }

  implicit def futureContext2res(p: Future[Context]): Res[Context] = new Res[Context] {
    override def asTry(): Try[Context] = try {
      import duration._
      Await.ready(p, 60 seconds)
      p.value.get
    } catch {
      case th: Throwable => Failure(th)
    }
  }

  implicit def unit2res(p:Unit) = new Res[Unit] {
    override def asTry(): Try[Unit] = Try(p)
  }

  implicit def tryUnit2res(p: Try[Unit]): Res[Unit] = new Res[Unit] {
    override def asTry(): Try[Unit] = p
  }

  implicit def futureUnit2res(p: Future[Unit]): Res[Unit] = new Res[Unit] {
    override def asTry(): Try[Unit] = try {
      import duration._
      Await.ready(p, 60 seconds)
      p.value.get
    } catch {
      case th: Throwable => Failure(th)
    }
  }
}
