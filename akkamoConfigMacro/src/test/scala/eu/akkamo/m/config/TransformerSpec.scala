package eu.akkamo.m.config

import com.typesafe.config.ConfigFactory
import org.scalatest.{FlatSpec, Matchers}


class PointWithDef(val x: Int, val y: Int , val labelDV: String = "default")

case class PointWithDef2(x: Int, y: Int)(labelDV:String = "default")(val description: String = {s"$labelDV(x=$x, y= ${y})"})

/**
  * @author jubu.
  */
class TransformerSpec extends FlatSpec with Matchers {

  "base types" should "be transformed" in {
    val tr = implicitly[Transformer[Int]]

    val cfg = ConfigFactory.parseString(
      """
        | x = 1
      """.stripMargin)

    val res = tr.apply("x", cfg)

    assert(res == 1)
  }

  "config wrapper, when uses generated transformer" should "parse to instance of class with default values in constructor" in {
    val cfg = ConfigFactory.parseString("""point = {x = 1, y = 2}""")
    val tr = implicitly[Transformer[PointWithDef]]

    val pl = tr.apply("point", cfg)
    val pr = new PointWithDef(1, 2)
    assert(pl.x == pr.x)
    assert(pl.y == pr.y)
    assert(pl.labelDV == pr.labelDV)
  }

  "config wrapper, when uses generated transformer" should "parse to instance of case class with default values in constructor" in {
    val cfg = ConfigFactory.parseString("""point = {x = 1, y= 2, description = "yyy" }""")
    val tr = implicitly[Transformer[PointWithDef2]]

    val pl = tr.apply("point", cfg)
    val pr = PointWithDef2(x = 1, y = 2)()("yyy")
    assert(pl == pr)
  }

}
