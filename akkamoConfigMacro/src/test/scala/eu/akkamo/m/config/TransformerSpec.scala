package eu.akkamo.m.config

import java.util.Properties

import com.typesafe.config.ConfigFactory
import eu.akkamo.m.config.x.PointX
import org.scalatest.WordSpecLike


class PointWithDef(val x: Int, val y: Int, val labelDV: String = "default")

case class PointWithDef2(x: Int, y: Int)(labelDV: String = "default")(val description: String = {
  s"$labelDV(x=$x, y= ${y})"
})


trait PointHolder {

  case class Point(x: Int, y: Int)

}

case class Label(name: String)

case class PointWithLabel(x: Int, y: Int, z: Int = 0)(implicit val l: Label)


/**
  * @author jubu.
  */
class TransformerSpec extends WordSpecLike with PointHolder {

  "Config wrapper" when {

    "uses generated transformer" should {

      "parse base types" in {
        val tr = implicitly[Transformer[Int]]

        val cfg = ConfigFactory.parseString(
          """
            | x = 1
          """.stripMargin)

        val res = tr.apply("x", cfg)

        assert(res == 1)
      }

      "parse to instance of class with default values in constructor" in {
        val cfg = ConfigFactory.parseString("""point = {x = 1, y = 2}""")
        val tr = implicitly[Transformer[PointWithDef]]

        val pl = tr.apply("point", cfg)
        val pr = new PointWithDef(1, 2)
        assert(pl.x == pr.x)
        assert(pl.y == pr.y)
        assert(pl.labelDV == pr.labelDV)
      }

      "parse to instance of case class with default values in constructor" in {
        val cfg = ConfigFactory.parseString("""point = {x = 1, y= 2, description = "yyy" }""")
        val tr = implicitly[Transformer[PointWithDef2]]

        val pl = tr.apply("point", cfg)
        val pr = PointWithDef2(x = 1, y = 2)()("yyy")
        assert(pl == pr)
      }

      "parse to instance of case class defined inside module" in {
        //import PointHolder.Point
        val cfg = ConfigFactory.parseString("""point = {x = 1, y= 2}""")
        val tr = implicitly[Transformer[Point]]

        val pl = tr("point", cfg)
        val pr = Point(x = 1, y = 2)
        assert(pl == pr)
      }

      "parse to instance of case class having implicit parameters" in {
        implicit val l = Label("Implicit label")
        val cfg = ConfigFactory.parseString("""point = {x = 1, y= 2}""")
        val tr = implicitly[Transformer[PointWithLabel]]
        val pl = tr("point", cfg)
        val pr = PointWithLabel(x = 1, y = 2)
        assert(pl == pr)
      }

      "parse to instance of case class from diferent package" in {
        val cfg = ConfigFactory.parseString("""point = {x = 1, y= 2}""")
        val tr = implicitly[Transformer[PointX]]
        val pl = tr("point", cfg)
        val pr = PointX(x = 1, y = 2)()
        assert(pl == pr)
      }

      "parse to instance of Properties" in {
        val cfg = ConfigFactory.parseString("""props = {a.x = 1, a.y= "y"}""")
        val tr = implicitly[Transformer[Properties]]
        val pl = tr("props", cfg)
        assert(pl.size() == 2)
        assert(pl.getProperty("a.x") == "1")
      }


      "throw IllegalArgumentException if wrong input" in {
        implicit val l = Label("Implicit label")
        val cfg = ConfigFactory.parseString("""point = 2""")
        val tr = implicitly[Transformer[PointWithLabel]]
        assertThrows[Exception] {
          tr("point", cfg)
        }
      }
    }
  }
}
