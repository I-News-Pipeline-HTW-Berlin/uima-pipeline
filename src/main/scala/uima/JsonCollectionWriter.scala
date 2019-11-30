package uima

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.`type`.Lemma
import org.apache.uima.fit.component.JCasConsumer_ImplBase
import org.apache.uima.fit.util.JCasUtil
import org.apache.uima.jcas.JCas
import spray.json._
import DefaultJsonProtocol._
import de.tudarmstadt.ukp.dkpro.core.api.metadata.`type`.MetaDataStringField
import json.{JSONComposer, JSONParser}

class JsonCollectionWriter extends JCasConsumer_ImplBase {
  override def process(aJCas: JCas): Unit = {

    val lemmas = JCasUtil.select(aJCas, classOf[Lemma]).toArray.toList.asInstanceOf[List[Lemma]].map(lem => lem.getValue)
    val readingTime = JCasUtil.select(aJCas, classOf[MetaDataStringField]).toArray.toList.head.asInstanceOf[MetaDataStringField].getValue.toInt
    val originalArticle = aJCas.getView("META_VIEW").getDocumentText
    val data = JSONParser.parseAll(originalArticle)
    val jsonString = JSONComposer.compose(
      data("_id").asInstanceOf[String],
      data("authors").asInstanceOf[List[String]],
      data("crawl_time").asInstanceOf[String],
      data("text").asInstanceOf[String],
      data("news_site").asInstanceOf[String],
      data("links").asInstanceOf[List[String]],
      data("published_time").asInstanceOf[String],
      data("keywords").asInstanceOf[List[String]],
      data("long_url").asInstanceOf[String],
      data("intro").asInstanceOf[String],
      data("title").asInstanceOf[String],
      data("image_links").asInstanceOf[List[String]],
      data("description").asInstanceOf[String],
      lemmas,
      readingTime)
    println(jsonString)
  }
}
