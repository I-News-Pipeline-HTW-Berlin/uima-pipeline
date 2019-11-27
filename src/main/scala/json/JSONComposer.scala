package json

import spray.json._
import json.ExtendedArticle

object JSONComposer {

  def compose(lemmas: List[String], readingTime: Int): String = {
    ExtendedArticle(lemmas, readingTime).toJson(ExtendedArticleJsonProtocol.ExtendedArticleJsonFormat).compactPrint
  }

}
