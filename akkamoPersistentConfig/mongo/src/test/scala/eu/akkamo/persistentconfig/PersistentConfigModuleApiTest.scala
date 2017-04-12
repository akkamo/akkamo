package eu.akkamo.persistentconfig

import eu.akkamo.{Akkamo, Context, Dependency, Initializable, Module}
import org.scalatest._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Try


/**
  * @note This test require running mongo on localhost on default port, database should be empty
  * @author jubu.
  */
class PersistentConfigModuleApiTest extends AsyncFlatSpec with BeforeAndAfter {

  import implicits._


  val akkamo = new Akkamo()

  val data = akkamo.run()

  val intKey = "test.intValue"
  val boolKey = "test.boolValue"
  val stringArrayKey = "test.stringArrayValue"

  s"in application.conf defined property: $intKey" should "equals 1" in {
    val res = TestModule.pc.get[Int](intKey)
    res.map {v =>
      assert(v == 1)
    }
  }

  s"in application.conf defined property: $boolKey" should "equals true" in {
    val res = TestModule.pc.get[Boolean](boolKey)
    res.map { v => assert(v == true) }
  }

  s"in application.conf defined property: $stringArrayKey" should "equals true" in {
    val res = TestModule.pc.get[List[String]](stringArrayKey)
    res.map { v => assert(v == List("Ahoj", "Ahoj")) }
  }

  s"in application.conf defined property: $intKey" should "be stored to db" in {
    TestModule.pc.store(intKey, 2)
      .flatMap { p =>
        TestModule.pc.get[Int](intKey)
      }.map { v =>
      assert(v == 2)
    }
  }



  after {
    akkamo.dispose(data)
  }
}

class TestModule extends Module with Initializable {

  override def dependencies(dependencies: Dependency): Dependency = dependencies.&&[PersistentConfig]

  override def initialize(ctx: Context) = Try {
    TestModule.pc = ctx.get[PersistentConfig]
    ctx
  }

}

object TestModule {
  var pc: PersistentConfig = null
}