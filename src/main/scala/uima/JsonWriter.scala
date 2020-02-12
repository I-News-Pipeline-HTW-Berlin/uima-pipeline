package uima

import com.typesafe.config.ConfigFactory
import db.DbConnector
import de.tudarmstadt.ukp.dkpro.core.api.metadata.`type`.MetaDataStringField
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.`type`.Lemma
import departmentsMapping.DepartmentMapping
import json.{JSONComposer, JSONParser}
import org.apache.spark.rdd.RDD
import org.apache.uima.fit.component.JCasConsumer_ImplBase
import org.apache.uima.fit.descriptor.SofaCapability
import org.apache.uima.fit.util.JCasUtil
import org.apache.uima.jcas.JCas
import org.mongodb.scala.bson.collection.mutable.Document
import org.mongodb.scala.{MongoClient, MongoCollection}

/**
 * Combines the contents of the original json document and the outcomes of the analysis (lemmas, reading time,
 * departments, most relevant lemmas) and writes the new json file into db.
 */
@SofaCapability(inputSofas = Array("MOST_RELEVANT_VIEW"))
class JsonWriter extends JCasConsumer_ImplBase {

  /**
   * Path to file containing department mapping
   */
  val departmentsPath: String = ConfigFactory.load().getString("app.departmentslocation")

  /**
   * Username for target db
   */
  val userName: String = ConfigFactory.load().getString("app.targetuser")

  /**
   * Password to target db
   */
  val pw: String = ConfigFactory.load().getString("app.targetpw")

  /**
   * Server address of target db
   */
  val serverAddress: String = ConfigFactory.load().getString("app.targetserver")

  /**
   * Port of target db
   */
  val port: String = ConfigFactory.load().getString("app.targetport")

  /**
   * Name of target db
   */
  val db: String = ConfigFactory.load().getString("app.targetdb")

  /**
   * Name of target collection
   */
  val collectionName: String = ConfigFactory.load().getString("app.targetcollection")

  /**
   * Department dictionary
   */
  val depKeywordsMapping: RDD[(String, List[String])] = DepartmentMapping.deserialize(departmentsPath)

  /**
   * The client for MongoDb
   */
  val mongoClient: MongoClient = DbConnector.createClient(userName, pw, serverAddress, port, db)

  /**
   * The target collection to write documents to
   */
  val collection: MongoCollection[Document] = DbConnector.getCollectionFromDb(db, collectionName, mongoClient)

  /**
   * Takes all outcomes of the main pipeline as well as the original article, creates a new json String containing all
   * information and writes each document into db.
   * @param aJCas
   */
  override def process(aJCas: JCas): Unit = {

    // gets all lemmas of the document and maps them to their value
    val lemmas: List[String] = JCasUtil.select(aJCas, classOf[Lemma])
      .toArray
      .toList.asInstanceOf[List[Lemma]]
      .map(lem => lem.getValue)

    // get reading time of the document
    val readingTime: Int = JCasUtil.select(aJCas, classOf[MetaDataStringField])
      .toArray
      .toList
      .head.asInstanceOf[MetaDataStringField]
      .getValue
      .toInt

    // gets view containing most relevant lemmas
    val mostRelevantView: JCas = aJCas.getView("MOST_RELEVANT_VIEW")

    // gets most relevant lemmas of the document and maps them to their value
    val mostRelevantLemmas: List[String] = JCasUtil.select(mostRelevantView, classOf[Lemma])
      .toArray
      .toList.asInstanceOf[List[Lemma]]
      .map(lem => lem.getValue)

    // gets original json and parses it
    val originalArticle = aJCas.getView("META_VIEW").getDocumentText
    val data = JSONParser.parseOriginalArticle(originalArticle)

    // mapping departments
    val departments: List[String] = DepartmentMapping.getDepartmentsForArticle(data("keywords").asInstanceOf[List[String]], depKeywordsMapping)
      .toList

    // composing new json string
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

    // write document to db
    DbConnector.writeSingleDocumentToCollection(collection, jsonString)
  }
}