package eu.akkamo

import org.scalatest.{FlatSpec, Matchers}

/**
  * ''ScalaTest'' specification for the [[ParamsReplacer]] object.
  *
  * @author Vaclav Svejcar (vaclav.svejcar@gmail.com)
  */
class ParamsReplacerSpec extends FlatSpec with Matchers {

  "ParamsReplacer" should "properly replace all parameters" in {
    val input: String = "Hello, %{name} %{surname}, do you know where the Doctor is?"
    val expected: String = "Hello, John Smith, do you know where the Doctor is?"
    val replacements: Map[String, String] = Map(
      "name" -> "John",
      "surname" -> "Smith"
    )

    val replaced: String = ParamsReplacer.replaceParams(input, replacements)

    replaced should equal(expected)
  }

}
