package uima


import de.tudarmstadt.ukp.dkpro.core.api.segmentation.`type`.Lemma
import org.apache.uima.fit.component.JCasConsumer_ImplBase
import org.apache.uima.fit.util.JCasUtil
import org.apache.uima.jcas.JCas
import de.tudarmstadt.ukp.dkpro.core.api.metadata.`type`.MetaDataStringField
import json.{JSONComposer, JSONParser}
import org.apache.uima.fit.descriptor.ConfigurationParameter

class JsonWriter extends JCasConsumer_ImplBase {


  override def process(aJCas: JCas): Unit = {

    val lemmas = JCasUtil.select(aJCas, classOf[Lemma]).toArray.toList.asInstanceOf[List[Lemma]].map(lem => lem.getValue)
    val readingTime = JCasUtil.select(aJCas, classOf[MetaDataStringField]).toArray.toList.head.asInstanceOf[MetaDataStringField].getValue.toInt
    val originalArticle = aJCas.getView("META_VIEW").getDocumentText
    val data = JSONParser.parseAll(originalArticle)
    val jsonString = JSONComposer.compose(
      data("_id").asInstanceOf[String],
      data("authors").asInstanceOf[List[String]],
      data("crawl_time").asInstanceOf[BigDecimal],
      data("text").asInstanceOf[String],
      data("news_site").asInstanceOf[String],
      data("links").asInstanceOf[List[String]],
      if (data("published_time") != null) data("published_time").asInstanceOf[BigDecimal] else null,
      data("keywords").asInstanceOf[List[String]],
      data("long_url").asInstanceOf[String],
      data("short_url").asInstanceOf[String],
      if (data("intro") != null) data("intro").asInstanceOf[String] else null,
      data("title").asInstanceOf[String],
      data("image_links").asInstanceOf[List[String]],
      if (data("description") != null) data("description").asInstanceOf[String] else null,
      lemmas,
      readingTime)
      //println(jsonString)
      val metaDataStringField = new MetaDataStringField(aJCas, 0, originalArticle.size-1)
      metaDataStringField.setKey("json")
      metaDataStringField.setValue(jsonString)
      metaDataStringField.addToIndexes()
  }
}
