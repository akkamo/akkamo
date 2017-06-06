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

    val transformParam = (p: c.universe.Symbol) => {
      val pTerm: c.universe.TermSymbol = p.asTerm
      val name = pTerm.name.decodedName.toString.stripPrefix("`").stripSuffix("`")
      val res =
        q"""implicitly[Transformer[${pTerm.typeSignature}]].apply($name, o)"""
      res
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
            val params = ps.map(transformParam)
            params
          }
          q"new ${tpe}(...${ps})" // cal constructor with parameters
        case companion => // there is a companion object
          val applyMethod = companion.member(TermName("apply")).asMethod
          val params = applyMethod.paramLists.flatten.map(transformParam)
          q"${applyMethod}(..$params)"
      }
      val typeName = TermName(tpe.toString)
      val r = q"""
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
