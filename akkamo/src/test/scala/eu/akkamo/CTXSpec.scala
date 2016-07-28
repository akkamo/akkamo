package eu.akkamo

import org.scalatest.{FlatSpec, Matchers}

/**
  * @author jubu
  */
class CTXSpec extends FlatSpec with Matchers {

  case class Bean1(k:Int, v:String)

  val first1 = Bean1(1, "First")
  val second1 = Bean1(2, "Second")

  val ctx1  = {
    val ctx = new CTX()
    val ctx2 = ctx.register(first1)
    ctx2.register(second1, Some(second1.v))
  }

  case class Bean2(k:Int, v:String)

  val first2 = Bean2(1, "First")
  val second2 = Bean2(2, "Second")

  val ctxBeans = {
    val ctx2 = ctx1.register(first2)
    ctx2.register(second2, second2.v)
  }


  "CTX" should "inject an Bean" in {

    // no bean of Bean2 registered
    assert(ctx1.inject[Bean2] == None)

    assert(ctxBeans.inject[Bean1] == Some(first1))

    assert(ctxBeans.get[Bean1] == first1)

    assert(ctxBeans.get[Bean1](second1.v) == second1)


    assert(ctxBeans.inject[Bean2] == Some(first2))

    assert(ctxBeans.get[Bean2] == first2)

    assert(ctxBeans.get[Bean2](second2.v) == second2)

  }

  "CTX" should "throw exception when duplicity during injection detected" in {
    an[ContextError] should be thrownBy ctxBeans.register(second2, second2.v)
  }

  case class MyRegistry(k:String, data: List[String] = Nil) extends Registry[String] {

    override def copyWith(p: String): MyRegistry.this.type = this.copy(data = p::this.data).asInstanceOf[this.type]
  }

  val defaultRegistry = MyRegistry("default")
  val secondRegistry = MyRegistry("second")

  val ctxRegistry = {

    val ctx1 = ctxBeans.register(defaultRegistry)
    val ctx2 = ctx1.register(defaultRegistry, defaultRegistry.k)
    ctx2.register(secondRegistry, secondRegistry.k)
  }

  "CTX" should "after registerIn should contains keep consistent Registries" in {
    val ctx = ctxRegistry.registerIn[MyRegistry, String]("Hi")
    assert(ctx.get[MyRegistry].data == List("Hi"))
    assert(ctx.get[MyRegistry](defaultRegistry.k).data == List("Hi"))

    val registered = ctx.registered[MyRegistry]
    assert(registered.get(ctx.get[MyRegistry]) == Some(Set(defaultRegistry.k)))
  }

}
