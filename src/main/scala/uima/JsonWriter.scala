package uima


import com.typesafe.config.ConfigFactory
import db.DbConnector
import de.tudarmstadt.ukp.dkpro.core.api.metadata.`type`.MetaDataStringField
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.`type`.Lemma
import departmentsMapping.DepartmentMapping
import json.{JSONComposer, JSONParser}
import org.apache.uima.fit.component.JCasConsumer_ImplBase
import org.apache.uima.fit.descriptor.{ConfigurationParameter, SofaCapability}
import org.apache.uima.fit.util.JCasUtil
import org.apache.uima.jcas.JCas

@SofaCapability(inputSofas = Array("MOST_RELEVANT_VIEW"))
class JsonWriter extends JCasConsumer_ImplBase {

  //@ConfigurationParameter(name = JsonWriter.DEPARTMENTS_PATH)
  val departmentsPath: String = ConfigFactory.load().getString("app.departmentslocation")

  //@ConfigurationParameter(name = JsonWriter.USER_NAME)
  val userName: String = ConfigFactory.load().getString("app.targetuser")

  //@ConfigurationParameter(name = JsonWriter.PW)
  val pw: String = ConfigFactory.load().getString("app.targetpw")

  //@ConfigurationParameter(name = JsonWriter.SERVER_ADDRESS)
  val serverAddress: String = ConfigFactory.load().getString("app.targetserver")

  //@ConfigurationParameter(name = JsonWriter.PORT)
  val port: String = ConfigFactory.load().getString("app.targetport")

  //@ConfigurationParameter(name = JsonWriter.DB)
  val db: String = ConfigFactory.load().getString("app.targetdb")

  //@ConfigurationParameter(name = JsonWriter.COLLECTION_NAME)
  val collectionName: String = ConfigFactory.load().getString("app.targetcollection")

  //für departments:
  val depKeywordsMapping = DepartmentMapping.deserialize(departmentsPath)

  val mongoClient = DbConnector.createClient(userName, pw, serverAddress, port, db)
  val collection = DbConnector.getCollectionFromDb(db, collectionName, mongoClient)

  override def process(aJCas: JCas): Unit = {

    val lemmas = JCasUtil.select(aJCas, classOf[Lemma]).toArray.toList.asInstanceOf[List[Lemma]].map(lem => lem.getValue)
    val readingTime = JCasUtil.select(aJCas, classOf[MetaDataStringField]).toArray.toList.head.asInstanceOf[MetaDataStringField].getValue.toInt
    val mostRelevantView = aJCas.getView("MOST_RELEVANT_VIEW")
    val mostRelevantLemmas = JCasUtil.select(mostRelevantView, classOf[Lemma]).toArray.toList.asInstanceOf[List[Lemma]].map(lem => lem.getValue)
    val originalArticle = aJCas.getView("META_VIEW").getDocumentText
    val data = JSONParser.parseAll(originalArticle)
    val departments = DepartmentMapping.getDepartmentsForArticle(data("keywords").asInstanceOf[List[String]], depKeywordsMapping)
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
      readingTime,
      mostRelevantLemmas,
      departments)

    //versuch, event. wieder einkommentieren
      /*val metaDataStringField = new MetaDataStringField(aJCas, 0, originalArticle.size-1)
      metaDataStringField.setKey("json")
      metaDataStringField.setValue(jsonString)
      metaDataStringField.addToIndexes()*/
    DbConnector.writeSingleDocumentToCollection(collection, jsonString)
  }
}

/*object JsonWriter{
  final val DEPARTMENTS_PATH = "departmentsPath"
  final val USER_NAME = "userName"
  final val PW = "password"
  final val SERVER_ADDRESS = "serverAddress"
  final val PORT = "port"
  final val DB = "database"
  final val COLLECTION_NAME = "collectionName"
}*/