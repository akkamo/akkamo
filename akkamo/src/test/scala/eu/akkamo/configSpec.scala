package eu.akkamo

import com.typesafe.config.{Config, ConfigFactory, ConfigObject, ConfigValue}
import eu.akkamo.config.Transformer
import org.scalatest.{FlatSpec, Matchers}

/**
  * @author jubu.
  */
class configSpec extends FlatSpec with Matchers {
  import eu.akkamo.config
  import config.implicits._


  case class X(x: Int)

  implicit object CV2Type extends Transformer[X] {
    override def apply(v: ConfigValue): X = {
      implicit val cfg = v.asInstanceOf[ConfigObject].toConfig
      val x = config.as[Int]("x")
      X(x)
    }
  }


  private val cfg1 = ConfigFactory.parseString(
    """
      |ai = 1
      |ad = 1.0
      |ab = true
      |as = "ahoj"
      |ami = {
      | 1 = 1
      | 2 = 2
      |}
      |ali = [1, 2, 3]
      |amc = {
      | 1 = { x = 1}
      | 2 = { x = 2}
      |}
      |""".stripMargin
  )

  "config wrapper should" should "right return right" in {
    implicit val cfg = cfg1
    assert(config.as[BigDecimal]("ad") == BigDecimal(1.0))
    assert(config.as[Double]("ad") == 1.0)
    assert(config.as[Double]("ai") == 1)
    assert(config.as[String]("as") == "ahoj")
    assert(config.as[Map[String, Int]]("ami") == Map("1" -> 1, "2" -> 2))
    assert(config.as[List[Int]]("ali") == List(1, 2, 3))
    assert(config.as[Map[String, Config]]("amc") == Map("1" -> config.as[Config]("amc.1"), "2" -> config.as[Config]("amc.2")))
  }

  "config wrapper should" should "return right option" in {
    implicit val cfg = cfg1

    assert(config.asOpt[BigDecimal]("ad") == Some(BigDecimal(1.0)))
    assert(config.asOpt[Map[String, Config]]("amc") == Some(Map("1" -> config.as[Config]("amc.1"), "2" -> config.as[Config]("amc.2"))))
  }

  private val cfg2 = ConfigFactory.parseString(
    """
      |aox = {x = 1}
      |aolx = [ {x = 1}, {x = 2}]
      |aomx = {
      | 1 = {x = 1}
      | 2 = {x = 2}
      |}
      |""".stripMargin
  )


  "config wrapper should" should "return right value for new defined custom converter" in {
    implicit val cfg = cfg2

    assert(config.as[X]("aox") == X(1))
    assert(config.as[List[X]]("aolx") == List(X(1), X(2)))
    assert(config.as[Map[String, X]]("aomx") == Map("1"->X(1), "2"->X(2)))

  }
}
