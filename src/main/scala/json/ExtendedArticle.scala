package json

import spray.json.{DefaultJsonProtocol, JsArray, JsNumber, JsObject, JsString, JsValue, RootJsonFormat}

// alles mitschleifen!!
// nur noch nicht verarbeitete artikel analysieren (crawl time)

//TODO muss erweitert werden (ist aktuell nur eine Testversion)
case class ExtendedArticle(val lemmas: List[String], val readingTime: Int) {
  def lemmasAsJsStrings = lemmas.map(l => JsString(l)).toVector
  override def toString: String = lemmas.reduceLeft((l1,l2) => l1+l2) + readingTime.toString
}

object ExtendedArticleJsonProtocol extends DefaultJsonProtocol{
  implicit val articleFormat = jsonFormat2(ExtendedArticle)

  implicit object ExtendedArticleJsonFormat extends RootJsonFormat[ExtendedArticle] {
    def write(ea: ExtendedArticle) = JsObject(
      "lemmas" -> JsArray(ea.lemmasAsJsStrings),
      "readingTime" -> JsNumber(ea.readingTime)
    )

    override def read(json: JsValue): ExtendedArticle = ???
  }
}

