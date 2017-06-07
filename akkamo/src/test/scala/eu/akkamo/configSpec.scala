package eu.akkamo

import com.typesafe.config.{Config, ConfigFactory, ConfigObject, ConfigValue}
import eu.akkamo.m.config._
import org.scalatest.{FlatSpec, Matchers}

case class Point(x: Int, y: Int, label: Option[String])

case class Points(map: Map[String, Point])

case class PointWithDef(val x: Int, val y: Int , val labelDV: String = "default")

private[akkamo] case class ClassHolder(`class`:String)


/**
  * @author jubu.
  */
class configSpec extends FlatSpec with Matchers {

  import config.implicits._


  case class X(x: Int)

  class Point2(val x: Int, val y: Int)(val label: String)


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
    assert(config.as[Map[String, X]]("aomx") == Map("1" -> X(1), "2" -> X(2)))

  }

  "config wrapper, when uses generated transformer" should "parse to instance of Point" in {
    implicit var cfg = ConfigFactory.parseString(
      """point = {
        | x = 1
        | y = 2
        | label = "ahoj"
        |}""".stripMargin)

    implicit val trp: Transformer[Point] = config.generateTransformer[Point]

    assert(trp != null)

    assert(config.as[Point]("point") == Point(1, 2, Some("ahoj")))

    cfg = ConfigFactory.parseString("""point = {x = 1, y = 2}""")

    assert(config.as[Point]("point") == Point(1, 2, None))
  }

  "config wrapper, when uses generated transformer" should "parse to instance of Points" in {

    implicit val cfg = ConfigFactory.parseString(
      """
        |points = {
        | map = {
        |   p1 = {x = 1, y = 2, label = "ahoj"}
        |   p2 = {x = 2, y = 2}
        | }
        |}""".stripMargin)

    implicit val trp: Transformer[Point] = config.generateTransformer
    implicit val trps: Transformer[Points] = config.generateTransformer

    assert(config.as[Points]("points") == Points(Map("p1" -> Point(1, 2, Some("ahoj")), "p2" -> Point(2, 2, None))))
  }


  "config wrapper, when uses generated transformer" should "parse to instance of class with two parameter lists" in {

    implicit val cfg = ConfigFactory.parseString("""point = {x = 1, y = 2, label = "ahoj" }""")
    implicit val trp: Transformer[Point2] = config.generateTransformer[Point2]


    val pl = config.as[Point2]("point")
    val pr = new Point2(1, 2)("ahoj")
    assert(pl.x == pr.x)
    assert(pl.y == pr.y)
    assert(pl.label == pr.label)
  }

  "config wrapper, when uses generated transformer" should "parse to instance of class with default values in constructor" in {

    implicit val cfg = ConfigFactory.parseString("""point = {x = 1, y = 2}""")
    implicit val trp: Transformer[PointWithDef] = config.generateTransformer[PointWithDef]


    val pl = config.as[PointWithDef]("point")
    val pr = new PointWithDef(1, 2, "default")
    assert(pl.x == pr.x)
    assert(pl.y == pr.y)
    assert(pl.labelDV == pr.labelDV)
  }



  "config wrapper, when uses generated transformer" should "parse to instance of class having parameter named: `class` " in {
    implicit val cfg = ConfigFactory.parseString("""classHolder = { class = "xxx" }""")
    implicit val trp: Transformer[ClassHolder] = config.generateTransformer[ClassHolder]

    val pl = config.as[ClassHolder]("classHolder")
    val pr = new ClassHolder("xxx")
    assert(pl == pr)
  }
}
