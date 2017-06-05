package eu.akkamo

import com.typesafe.config.{Config, ConfigFactory}
import eu.akkamo.m.config.Transformer
import org.scalatest.{FlatSpec, Matchers}

case class Foo(x: Int)

/**
  * @author jubu.
  */
class InitializableSpec extends FlatSpec with Matchers {

  import eu.akkamo.config.implicits._

  implicit val cf2Foo: Transformer[Foo] = config.generateTransformer[Foo]

  "parsed config" should "return right values" in {

    val cfg1 = ConfigFactory.parseString(
      """
        |foos {
        | a1 = {x = 1}
        | a2 = {
        |   default = true
        |   aliases = ["a3"]
        |   x = 2
        | }
        |}
        |""".stripMargin
    )

    val desired = List((false, List("a1"), Foo(1)), (true, List("a2", "a3"), Foo(2)))
    val parsed = Initializable.parseConfig[Foo]("foos", cfg1)
    assert(parsed.getOrElse(Nil) == desired)
  }

  "validation of parsed config" should "return (false, false, true) when empty" in {

    implicit val cfg = ConfigFactory.parseString(
      """
        |foos {
        |}
        |""".stripMargin
    )

    val parsed = Initializable.parseConfig[Foo]("foos")
    val r = Initializable.validate(parsed.get)
    val c = (false, false, true)
    assert(r == c)
  }

  "validation of parsed config" should "return (true, false, true) when multiple defaults" in {

    implicit  val cfg = ConfigFactory.parseString(
      """
        |foos {
        | a1 = {
        |   default = true
        |   x = 1
        | }
        | a2 = {
        |   default = true
        |   aliases = ["a3"]
        |   x = 2
        | }
        |}
        |""".stripMargin
    )

    val parsed = Initializable.parseConfig[Foo]("foos")
    val r = Initializable.validate(parsed.get)
    val c = (true, false, true)
    assert(r == c)
  }

  "validation of parsed config" should "return (true, false, true) when default is missing" in {

    implicit  val cfg = ConfigFactory.parseString(
      """
        |foos {
        | a1 = {
        |   x = 1
        | }
        | a2 = {
        |   aliases = ["a3"]
        |   x = 2
        | }
        |}
        |""".stripMargin
    )

    val parsed = Initializable.parseConfig[Foo]("foos")
    val r = Initializable.validate(parsed.get)
    val c = (true, false, true)
    assert(r == c)
  }

  "validation of parsed config" should "return (true, true, false) when ambigious aliases detected" in {

    implicit  val cfg = ConfigFactory.parseString(
      """
        |foos {
        | a1 = {
        |   default = true
        |   x = 1
        | }
        | a2 = {
        |   aliases = ["a1"]
        |   x = 2
        | }
        |}
        |""".stripMargin
    )

    val parsed = Initializable.parseConfig[Foo]("foos")
    val r = Initializable.validate(parsed.get)
    val c = (true, true, false)
    assert(r == c)
  }



  "parsed config to `Config`" should "return right value" in {
    implicit val cfg = ConfigFactory.parseString(
      """
        |akkamo.akka = {
        |  name1{
        |    aliases = ["alias1", "alias2"]
        |   	akka{
        |   	  loglevel = "DEBUG"
        |     	debug {
        |     	  lifecycle = on
        |    	}
        |    }
        |  }
        |  name2{
        |    default = true
        |  }
        |}
        |""".stripMargin
    )
    val res = Initializable.parseConfig[Config]("akkamo.akka").get

    // map is in reverse order
    assert(res(0)._3.hasPath("akka") == false)
    assert(res(1)._3.hasPath("akka") == true)
  }
}
