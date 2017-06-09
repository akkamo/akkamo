package eu.akkamo.m.config

/**
  * @author jubu.
  */
object TransformerGenerator {

  import scala.language.experimental.macros
  import scala.reflect.macros.blackbox.Context

  def generate[T]: Transformer[T] = macro buildTransformer[T]

  def buildTransformer[T: c.WeakTypeTag](c: Context) = {
    import c.universe._

    val tpe: c.universe.Type = weakTypeOf[T]

    def transformParam = (s: c.universe.Symbol) => {
      val term =  s.asTerm
      val name = term .name.decodedName.toString.stripPrefix("`").stripSuffix("`")
      val res = q"""${term .name} = implicitly[Transformer[${term .typeSignature}]].apply($name, o)"""
      res
    }

    def createInstance(method: c.universe.MethodSymbol, tpe: c.universe.Type) = {
      // convert
      val ps: List[List[c.universe.Tree]] = method.paramLists
        .map{list=>
          list.filter(!_.asTerm.isParamWithDefault)
        }.map(_.map(transformParam))
      if(method.isConstructor) {
        q"new ${tpe}(...${ps})" // call constructor
      } else {
        q"${method}(...${ps})" // call apply with params
      }
    }

    def findConstructor(tpe: c.universe.Type) = tpe.members.find(_.isConstructor).map(_.asMethod)

    def findApplyMethod(companion: c.universe.Type) = {
      val apply = companion.member(TermName("apply"))
      apply match {
        case NoSymbol => None
        case res => Some(res.asMethod)
      }
    }

   def buildClass(tpe:c.universe.Type) = {
      val instance = tpe.companion match {
        case NoType => // no companion object let find constructor
          val constructor = findConstructor(tpe).getOrElse(
            throw new Exception(
              s"""[error] Constructor could not be determined for [[${tpe}]].
                 |Also this may be due to a bug in scalac (SI-7567) that arises when a case class within a function is derive.
                 |As a workaround, move the declaration to the module-level.""".stripMargin)
          )
          createInstance(constructor, tpe)
        case companion => // there is a companion object
          val applyMethodorConstructor = findApplyMethod(companion).getOrElse(
            findConstructor(tpe).getOrElse(
              throw new Exception(
                s"""[error] The apply method from companion object, or regular constructor could not be determined for [[${tpe}]].
                   |This may be due to a bug in scalac (SI-7567) that arises when a case class within a function is derive.
                   |As a workaround, move the declaration to the module-level.""".stripMargin)

            ))
          createInstance(applyMethodorConstructor, tpe)
      }
      val typeName = TermName(tpe.toString)
      val r =
        q"""
        new Transformer[${tpe}] {
          import com.typesafe.config.ConfigValue
          override def apply(obj: ConfigValue): ${tpe} = {
            assert(obj.valueType() == com.typesafe.config.ConfigValueType.OBJECT,
            "Only ConfigObject instance can be converted to:" +  ${typeName.toString})
            val o = obj.asInstanceOf[com.typesafe.config.ConfigObject]
            $instance
          }
        }"""
      r
    }
    try {
      buildClass(tpe)
    } catch {
      case th: Throwable => c.abort(c.enclosingPosition, th.getMessage)
    }
  }
}
