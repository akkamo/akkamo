package eu.akkamo

import org.scalatest.{FlatSpec, Matchers}

import scala.collection.mutable
import scala.util.Try


/**
  * @author jubu
  */
class AkkamoSpec extends FlatSpec with Matchers {

  type IO = mutable.MutableList[Initializable]
  type DO = mutable.MutableList[Runnable]

  abstract class P(implicit initOut: IO, runOut: DO) extends Module with Initializable with Runnable {
    override def initialize(ctx: Context) = Try {
      this +=: initOut
      ctx
    }

    override def run(ctx: Context) = Try {
      this +=: runOut
      ctx
    }
  }

  def build(implicit initOut: IO, disposeOut: DO) = {

    class E extends P {
      override def dependencies(dependencies: Dependency): Dependency = dependencies.&&[A]
    }

    class D extends P {
      override def dependencies(dependencies: Dependency): Dependency = dependencies
    }
    class C extends P {
      override def dependencies(dependencies: Dependency): Dependency = dependencies.&&[D]
    }

    class B extends P {
      override def dependencies(dependencies: Dependency): Dependency = dependencies.&&[D]
    }


    class A extends P {
      override def dependencies(dependencies: Dependency): Dependency = dependencies.&&[D].&&[C].&&[B]
    }


    class AE extends P {
      override def dependencies(dependencies: Dependency): Dependency = dependencies.&&[D].&&[C].&&[B].&&[E]
    }

    (new A, new B, new C, new D, new AE)
  }

  class DeadCtx extends Module with Initializable {

    override def initialize(ctx: Context): Res[Context] = Try {
      ctx.register("Caf")
      ctx
    }

    override def dependencies(dependencies: Dependency): Dependency = dependencies
  }

  class DeadCtx2 extends Module with Runnable {

    override def run(ctx: Context): Res[Context] = Try {
      ctx.register("Caf")
      ctx
    }
    override def dependencies(dependencies: Dependency): Dependency = dependencies
  }


  "AkkamoRun" should "throw exception when dependency is missing" in {
    implicit val (l, r) = (new IO, new DO)
    val (a, _, c, d, _) = build
    val akkamo = new Akkamo
    val ctx = new CTX
    an[InitializationError] should be thrownBy akkamo.run(ctx, List(c, a, d))
  }

  "AkkamoRun" should "throw exception when cycle is detected" in {
    implicit val (l, r) = (new IO, new DO)
    val (_, b, c, d, ae) = build
    val akkamo = new Akkamo
    val ctx = new CTX
    an[InitializationError] should be thrownBy akkamo.run(ctx, List(c, ae, b, d))
  }

  "AkkamoRun" should "have a right order during init" in {
    implicit val (l, r) = (new IO, new DO)
    val (a, b, c, d, _) = build
    val akkamo = new Akkamo
    val ctx = new CTX
    akkamo.run(ctx, List(c, a, b, d))
    assert(l == List(a, b, c, d))
    assert(r == List(d, c, b, a))
  }

  "AkkamoInit" should "throw exception if unused context is created" in {
    System.setProperty(Akkamo.Strict, "true")
    val akkamo = new Akkamo
    val ctx = new CTX
    an[InitializationError] should be thrownBy akkamo.run(ctx, List(new DeadCtx))
    System.setProperty(Akkamo.Strict, "false")
  }


  "AkkamoRun" should "throw exception if unused context is created" in {
    System.setProperty(Akkamo.Strict, "true")
    val akkamo = new Akkamo
    val ctx = new CTX
    an[RunError] should be thrownBy akkamo.run(ctx, List(new DeadCtx2))
    System.setProperty(Akkamo.Strict, "false")
  }
}
