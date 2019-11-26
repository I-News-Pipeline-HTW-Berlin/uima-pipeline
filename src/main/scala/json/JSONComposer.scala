package json

import spray.json._

object JSONComposer {

  def compose(lemmas: List[String], readingTime: Int): String = {
    ExtendedArticle(lemmas, readingTime).toJson(JsonWriter[ExtendedArticle]).compactPrint
  }

}
