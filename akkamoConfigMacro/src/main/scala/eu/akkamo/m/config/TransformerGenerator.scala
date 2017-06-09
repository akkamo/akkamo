package eu.akkamo.m.config

/**
  * @author jubu.
  */
object TransformerGenerator {

  import scala.language.experimental.macros
  import scala.reflect.macros.blackbox.Context

  trait AT[T] extends Transformer[T] {
    import com.typesafe.config.ConfigObject
    @inline
    def a[V](key:String, t:Transformer[V])(implicit o:ConfigObject):Option[V] = try {
      Option(t(key, o))
    } catch { case _:NullPointerException => None }
  }

  def generate[T]: Transformer[T] = macro buildTransformer[T]

  def buildTransformer[T: c.WeakTypeTag](c: Context) = {
    import c.universe._

    val tpe: c.universe.Type = weakTypeOf[T]

    def makeVals(tpe: c.universe.Type, parameterLists:List[List[(c.universe.TermSymbol, Int, Int)]]) = (p:(c.universe.TermSymbol, Int, Int)) => {
      val (term, group, index)  = p
      val name = term.name.decodedName.toString.stripPrefix("`").stripSuffix("`")
      val vName = TermName(s"$$_$index")
      if (term.isParamWithDefault) {
        val companion = tpe.companion
        val defaultOpt: Option[c.universe.Symbol] = companion.members.find { p =>
          val method = p.asMethod
          val name = method.name.decodedName.toString
          name.endsWith(s"$$default$$${index}") // dirty  hack
        }
        val default = defaultOpt.getOrElse(throw new Exception(s"For type: [[${tpe}]] method for parameter: ${term.name} can't be resolved"))
        val defaultParameterLists = parameterLists.take(group).map(_.map{ p=>TermName(s"$$_${p._3}")})
        val invokeDefault = q"""${default}(...${defaultParameterLists})"""
        val res = q"""val $vName:${term.typeSignature} = a($name, implicitly[Transformer[${term.typeSignature}]]).getOrElse(${invokeDefault})"""
        res
      } else {
        val res =
          q"""val $vName = implicitly[Transformer[${term.typeSignature}]].apply($name, o)"""
        res
      }
    }

    val makeParameter = (p:(c.universe.TermSymbol, Int, Int)) => {
      val (term, _, index) = p
      val vName = TermName(s"$$_$index")
      q"${term.name} = ${vName}"
    }

    def createInstance(method: c.universe.MethodSymbol, tpe: c.universe.Type) = {
      // convert
      val parameterAsTermLists = indexedParameters(method.paramLists.map(_.map(_.asTerm)))
      val valList = parameterAsTermLists.map(_.map(makeVals(tpe, parameterAsTermLists))).flatten
      val parameterLists = parameterAsTermLists.map(_.map(makeParameter))
      if(method.isConstructor) {
        q"""
          ..$valList
          new ${tpe}(...${parameterLists})""" // call constructor
      } else {
        q"""
           ..$valList
           ${method}(...${parameterLists})""" // call apply with params
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

    def indexedParameters(lss: List[List[c.universe.TermSymbol]]):List[List[(c.universe.TermSymbol, Int, Int)]] = {
      val empty:List[List[(c.universe.TermSymbol, Int, Int)]] = Nil
      lss.zipWithIndex.foldLeft((empty, 0)){case ((res, index), (o, group)) =>
        var idx = index
        (o.map(p=>(p.asTerm, group, {idx +=1; idx}))::res, idx)
      }._1.reverse
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
        new eu.akkamo.m.config.TransformerGenerator.AT[${tpe}] {
          import com.typesafe.config.ConfigValue
          import com.typesafe.config.ConfigObject
          override def apply(obj: ConfigValue): ${tpe} = {
            assert(obj.valueType() == com.typesafe.config.ConfigValueType.OBJECT,
            "Only ConfigObject instance can be converted to:" +  ${typeName.toString})
            implicit val o:ConfigObject = obj.asInstanceOf[ConfigObject]
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
