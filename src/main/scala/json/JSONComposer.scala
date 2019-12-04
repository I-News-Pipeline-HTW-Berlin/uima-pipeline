package json

import java.util.Date

import spray.json._
import json.ExtendedArticle

object JSONComposer {

  def compose(id: String,
              authors: List[String],
              crawlTime: BigDecimal,
              text: String,
              newsSite: String,
              links: List[String],
              publishedTime: BigDecimal,
              keywords: List[String],
              longUrl: String,
              intro: String,
              title: String,
              imageLinks: List[String],
              description: String,
              lemmas: List[String],
              readingTime: Int): String = {

    ExtendedArticle(
      id,
      authors,
      crawlTime,
      text,
      newsSite,
      links,
      publishedTime,
      keywords,
      longUrl,
      intro,
      title,
      imageLinks,
      description,
      lemmas,
      readingTime).toJson(ExtendedArticleJsonProtocol.ExtendedArticleJsonFormat).compactPrint
  }

}
