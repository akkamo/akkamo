
type ANO = PartialFunction[Int, String]

val xx:Array[ANO] =  Array(
  {case x if(x==3)  => "3"},{case x if(x==2)  => "2"})

val res = xx.foldLeft(Option.empty[ANO]){(l, r)=> l match {
    case None => Some(r)
    case Some(x) => Some(x orElse  r)
  }
}


res.map(_(3))








