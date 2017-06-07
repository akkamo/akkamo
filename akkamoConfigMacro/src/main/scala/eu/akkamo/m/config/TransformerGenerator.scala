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
    def transformParam(tpe: c.universe.Type) = (p:(c.universe.Symbol, Int)) => {
      val pTerm: c.universe.TermSymbol = p._1.asTerm
      val index = p._2 + 1
      val name = pTerm.name.decodedName.toString.stripPrefix("`").stripSuffix("`")
      if (pTerm.isParamWithDefault) {
        val defaultOpt: Option[c.universe.Symbol] = tpe.companion.members.find { p =>
          val method = p.asMethod
          val name = method.name.decodedName.toString
          name.equals(s"apply$$default$$${index}") // dirty  hack
        }
        val default = defaultOpt.getOrElse(throw new Exception(s"For type: [[${tpe}]] method for parameter: ${pTerm.name} can't be resolved"))
        val res = q"""${pTerm.name} =  scala.util.Try(implicitly[Transformer[${pTerm.typeSignature}]].apply($name, o)).toOption.getOrElse(${default})"""
        res
      } else {
        val res =
          q"""${pTerm.name} = implicitly[Transformer[${pTerm.typeSignature}]].apply($name, o)"""
        res
      }
    }
    try {
      val instance = tpe.companion match {
        case NoType => // no companion object let find constructor
          val constructor = tpe.members.find(_.isConstructor).getOrElse(
            throw new Exception(
              s"""[error] The companion object, or regular constructor could not be determined for [[${tpe}]].
                 |Check if [[${tpe}]] is case class.
                 |Also this may be due to a bug in scalac (SI-7567) that arises when a case class within a function is derive.
                 |As a workaround, move the declaration to the module-level.""".stripMargin)
          )

          val ps: List[List[c.universe.Tree]] = constructor.asMethod.paramLists.map { ps =>
            val params = ps.zipWithIndex.map(transformParam(tpe))
            params
          }
          q"new ${tpe}(...${ps})" // cal constructor with parameters
        case companion => // there is a companion object
          val applyMethod = companion.member(TermName("apply")).asMethod
          val params = applyMethod.paramLists.flatten.zipWithIndex.map(transformParam(tpe))
          q"${applyMethod}(..$params)"
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
    } catch {
      case th: Throwable => c.abort(c.enclosingPosition, th.getMessage)
    }
  }
}
