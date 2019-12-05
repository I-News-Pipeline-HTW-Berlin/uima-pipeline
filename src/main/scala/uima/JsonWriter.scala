package uima


import de.tudarmstadt.ukp.dkpro.core.api.segmentation.`type`.Lemma
import org.apache.uima.fit.component.JCasConsumer_ImplBase
import org.apache.uima.fit.util.JCasUtil
import org.apache.uima.jcas.JCas
import spray.json._
import de.tudarmstadt.ukp.dkpro.core.api.metadata.`type`.MetaDataStringField
import json.{JSONComposer, JSONParser}
import org.apache.uima.fit.descriptor.ConfigurationParameter
import db.DbConnector

class JsonWriter extends JCasConsumer_ImplBase {

  @ConfigurationParameter(name = JsonWriter.USER_NAME)
  val userName = "s0558478"

  @ConfigurationParameter(name = JsonWriter.PW)
  val pw = "1unch30n"

  @ConfigurationParameter(name = JsonWriter.SERVER_ADDRESS)
  val serverAddress = "hadoop05.f4.htw-berlin.de"

  @ConfigurationParameter(name = JsonWriter.PORT)
  val port = "27020"

  @ConfigurationParameter(name = JsonWriter.DB)
  val db = "s0558478"

  @ConfigurationParameter(name = JsonWriter.COLLECTION_NAME)
  val collectionName = "processed_articles"


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
      data("published_time").asInstanceOf[BigDecimal],
      data("keywords").asInstanceOf[List[String]],
      data("long_url").asInstanceOf[String],
      data("short_url").asInstanceOf[String],
      data("intro").asInstanceOf[String],
      data("title").asInstanceOf[String],
      data("image_links").asInstanceOf[List[String]],
      data("description").asInstanceOf[String],
      lemmas,
      readingTime)
      println(jsonString)
      val metaDataStringField = new MetaDataStringField(aJCas, 0, originalArticle.size-1)
      metaDataStringField.setKey("json")
      metaDataStringField.setValue(jsonString)
      metaDataStringField.addToIndexes()
    //DbConnector.writeSingleDocumentToCollection(collection, jsonString)

  }

}

  object JsonWriter {
    final val USER_NAME = "s0558478"
    final val PW = "1unch30n"
    final val SERVER_ADDRESS = "hadoop05.f4.htw-berlin.de"
    final val PORT = "27020"
    final val DB = "s0558478"
    final val COLLECTION_NAME = "processed_articles"
}
