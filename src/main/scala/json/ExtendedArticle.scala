package json

import spray.json.{DefaultJsonProtocol, JsArray, JsNull, JsNumber, JsObject, JsString, JsValue, RootJsonFormat}

/**
 * object to provide structure for jsonWriter
 * @param id
 * @param authors
 * @param crawlTime
 * @param text
 * @param newsSite
 * @param links
 * @param publishedTime
 * @param keywords
 * @param longUrl
 * @param shortUrl
 * @param intro
 * @param title
 * @param imageLinks
 * @param description
 * @param lemmas
 * @param readingTime
 * @param mostRelevantLemmas
 * @param departments
 */
case class ExtendedArticle(id: String,
                           authors: List[String],
                           crawlTime: BigDecimal,
                           text: String,
                           newsSite: String,
                           links: List[String],
                           publishedTime: Any,
                           keywords: List[String],
                           longUrl: String,
                           shortUrl: String,
                           intro: Any,
                           title: String,
                           imageLinks: List[String],
                           description: Any,
                           lemmas: List[String],
                           readingTime: Int,
                           mostRelevantLemmas: List[String],
                           departments: List[String]) {
  def lemmasAsJsStrings: Vector[JsString] = lemmas.map(l => JsString(l)).toVector
  def mostRelevantLemmasAsJsStrings: Vector[JsString] = mostRelevantLemmas.map(mrl => JsString(mrl)).toVector
  def authorsAsJsStrings: Vector[JsString] = authors.map(a => JsString(a)).toVector
  def linksAsJsStrings: Vector[JsString] = links.map(l => JsString(l)).toVector
  def keywordsAsJsStrings: Vector[JsString] = keywords.map(k => JsString(k)).toVector
  def imageLinksAsJsStrings: Vector[JsString] = imageLinks.map(il => JsString(il)).toVector
  def departmentsAsJsStrings: Vector[JsString] = departments.map(dep => JsString(dep)).toVector

  override def toString: String = lemmas.reduceLeft((l1,l2) => l1+l2) + readingTime.toString
}

object ExtendedArticleJsonProtocol extends DefaultJsonProtocol{

  implicit object ExtendedArticleJsonFormat extends RootJsonFormat[ExtendedArticle] {
    def write(ea: ExtendedArticle): JsObject = JsObject(
      "_id" -> JsObject(Map("$oid" -> JsString(ea.id))),
      "authors" -> JsArray(ea.authorsAsJsStrings),
      "crawlTime" -> JsObject(Map("$date" -> JsNumber(ea.crawlTime))),
      "text" -> JsString(ea.text),
      "newsSite" -> JsString(ea.newsSite),
      "links" -> JsArray(ea.linksAsJsStrings),
      ea.publishedTime match {
        case pt: BigDecimal => "publishedTime" -> JsObject(Map("$date" -> JsNumber(pt)))
        case _ => "publishedTime" -> JsNull
      },
      "keywords" -> JsArray(ea.keywordsAsJsStrings),
      "longUrl" -> JsString(ea.longUrl),
      ea.intro match {
        case i: String => "intro" -> JsString(i)
        case _ => "intro" -> JsNull
      },
      "title" -> JsString(ea.title),
      "imageLinks" -> JsArray(ea.imageLinksAsJsStrings),
      ea.description match {
        case d: String => "description" -> JsString(d)
        case _ => "description" -> JsNull
      },
      "lemmas" -> JsArray(ea.lemmasAsJsStrings),
      "readingTime" -> JsNumber(ea.readingTime),
      "mostRelevantLemmas" -> JsArray(ea.mostRelevantLemmasAsJsStrings),
      "departments" -> JsArray(ea.departmentsAsJsStrings)
    )

    override def read(json: JsValue): ExtendedArticle = ???
  }
}

