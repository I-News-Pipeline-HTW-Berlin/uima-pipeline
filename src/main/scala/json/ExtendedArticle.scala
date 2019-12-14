package json

import spray.json.{DefaultJsonProtocol, JsArray, JsNull, JsNumber, JsObject, JsString, JsValue, RootJsonFormat}

case class ExtendedArticle(val id: String,
                           val authors: List[String],
                           val crawlTime: BigDecimal,
                           val text: String,
                           val newsSite: String,
                           val links: List[String],
                           val publishedTime: Any,
                           val keywords: List[String],
                           val longUrl: String,
                           val shortUrl: String,
                           val intro: Any,
                           val title: String,
                           val imageLinks: List[String],
                           val description: Any,
                           val lemmas: List[String],
                           val readingTime: Int) {
  def lemmasAsJsStrings = lemmas.map(l => JsString(l)).toVector
  def authorsAsJsStrings = authors.map(a => JsString(a)).toVector
  def linksAsJsStrings = links.map(l => JsString(l)).toVector
  def keywordsAsJsStrings = keywords.map(k => JsString(k)).toVector
  def imageLinksAsJsStrings = imageLinks.map(il => JsString(il)).toVector

  //TODO toString muss erweitert werden
  override def toString: String = lemmas.reduceLeft((l1,l2) => l1+l2) + readingTime.toString
}

object ExtendedArticleJsonProtocol extends DefaultJsonProtocol{

  implicit object ExtendedArticleJsonFormat extends RootJsonFormat[ExtendedArticle] {
    def write(ea: ExtendedArticle) = JsObject(
      "_id" -> JsObject(Map("$oid" -> JsString(ea.id))),
      "authors" -> JsArray(ea.authorsAsJsStrings),
      "crawl_time" -> JsObject(Map("$date" -> JsNumber(ea.crawlTime))),
      "text" -> JsString(ea.text),
      "newsSite" -> JsString(ea.newsSite),
      "links" -> JsArray(ea.linksAsJsStrings),
      ea.publishedTime match {
        case pt: BigDecimal => "published_time" -> JsObject(Map("$date" -> JsNumber(pt)))
        case _ => "published_time" -> JsNull //JsObject(Map("$date" -> JsNull))
      },
      "keywords" -> JsArray(ea.keywordsAsJsStrings),
      "long_url" -> JsString(ea.longUrl),
      ea.intro match {
        case i: String => "intro" -> JsString(i)
        case _ => "intro" -> JsNull
      },
      "title" -> JsString(ea.title),
      "image_links" -> JsArray(ea.imageLinksAsJsStrings),
      ea.description match {
        case d: String => "description" -> JsString(d)
        case _ => "description" -> JsNull
      },
      "lemmas" -> JsArray(ea.lemmasAsJsStrings),
      "reading_time" -> JsNumber(ea.readingTime)
    )

    override def read(json: JsValue): ExtendedArticle = ???
  }
}

