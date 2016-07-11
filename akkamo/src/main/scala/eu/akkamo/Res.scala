package eu.akkamo

import scala.concurrent.{Await, Future, duration}
import scala.util.{Failure, Try}

/**
  * Represents result of [[eu.akkamo.Initializable#initialize]] or [[eu.akkamo.Runnable#run]] operations.
  * The ``Res`` carry an ``Try[Context]`` instance.
  */
trait Res[T] {
  def asTry():Try[T]
}

/**
  * provides implicit conversions from [[eu.akkamo.Res]] to [[scala.util.Try]] or [[scala.concurrent.Future]]
  */
private[akkamo] object Res {

  implicit def context2res(p:Context) = new Res[Context] {
    override def asTry(): Try[Context] = Try(p)
  }

  implicit def tryContext2res(p:Try[Context]) = new Res[Context] {
    override def asTry(): Try[Context] = p
  }

  implicit def futureContext2res(p:Future[Context]) = new Res[Context] {
    override def asTry(): Try[Context] = try {
      import duration._
      Await.ready(p, 60 seconds)
      p.value.get
    } catch {
      case th:Throwable => Failure(th)
    }
  }

  implicit def tryUnit2res(p:Unit) = new Res[Unit] {
    override def asTry(): Try[Unit] = Try(p)
  }


  implicit def tryUnit2res(p:Try[Unit]) = new Res[Unit] {
    override def asTry(): Try[Unit] = p
  }

  implicit def futureUnit2res(p:Future[Unit]) = new Res[Unit] {
    override def asTry(): Try[Unit] = try {
      import duration._
      Await.ready(p, 60 seconds)
      p.value.get
    } catch {
      case th:Throwable => Failure(th)
    }
  }
}


