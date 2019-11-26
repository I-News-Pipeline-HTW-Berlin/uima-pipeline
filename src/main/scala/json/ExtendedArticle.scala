package json

import spray.json.DefaultJsonProtocol

//TODO muss erweitert werden (ist aktuell nur eine Testversion)
case class ExtendedArticle(val lemmas: List[String], val readingTime: Int) {
  override def toString: String = lemmas.reduceLeft((l1,l2) => l1+l2) + readingTime.toString
}

object ExtendedArticleJsonProtocol extends DefaultJsonProtocol{
  implicit val articleFormat = jsonFormat2(ExtendedArticle)
}