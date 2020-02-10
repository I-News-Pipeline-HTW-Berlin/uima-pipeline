package uima

import java.io.File

import com.typesafe.config.ConfigFactory
import db.DbConnector
import db.Helpers._
import json.JSONParser
import org.apache.uima.cas.CAS
import org.apache.uima.fit.component.CasCollectionReader_ImplBase
import org.apache.uima.util.{Progress, ProgressImpl}
import org.mongodb.scala.MongoClient
import org.mongodb.scala.bson.BsonDateTime
import org.mongodb.scala.bson.collection.mutable.Document
import org.mongodb.scala.model.{Filters, Sorts}

import scala.io.Source

/**
 * Reader for first pipeline (writeModel)
 * Gets original collection from database, extracts unprocessed documents and inserts them into pipeline.
 * Does not write last crawl time.
 */
class ReaderFirstPipeline() extends CasCollectionReader_ImplBase {
    /**
     * name of db user
     */
    val userName: String = ConfigFactory.load().getString("app.user")

    /**
     * password for db user
     */
    val pw: String = ConfigFactory.load().getString("app.pw")

    /**
     * db server address
     */
    val serverAddress: String = ConfigFactory.load().getString("app.server")

    /**
     * db port
     */
    val port: String = ConfigFactory.load().getString("app.port")

    /**
     * name of db
     */
    val db: String = ConfigFactory.load().getString("app.db")

    /**
     * name of collection
     */
    val collectionName: String = ConfigFactory.load().getString("app.collection")

    /**
     * path to file with last processed crawl time
     */
    val fileLocation: String = ConfigFactory.load().getString("app.lastcrawltimefile")

    val mongoClient: MongoClient = DbConnector.createClient(userName, pw, serverAddress, port, db)

    val lastCrawlTime = getLastCrawlTime

    val docs: Seq[Document] = DbConnector.getCollectionFromDb(db, collectionName, mongoClient)
      .find(
        Filters.and(
          Filters.gt("crawl_time", lastCrawlTime),
          Filters.ne("text", ""),
          Filters.ne("title", null)))
      .sort(Sorts.ascending("crawl_time"))
      .limit(500).results()

    println("Analyzing "+docs.size+ " documents...")

    val iterator: Iterator[Document] = docs.iterator

    var mCurrentIndex: Int = 0

    /**
     * Reads last crawl time from given file path and converts it to BsonDateTime.
     * @return BsonDateTime
     */
    def getLastCrawlTime: BsonDateTime = {
      if (new File(fileLocation).exists) {
        val fileSource = Source.fromFile(fileLocation)
        if (fileSource.hasNext) {
          val dateAsString = fileSource.getLines().next()
          return new BsonDateTime(dateAsString.toLong)
        }
      }
      new BsonDateTime(0)
    }

    /**
     * Inserts documents into pipeline by setting document text of jcas.
     * The document text of an article consists of title, intro and text.
     * The original json will be carried through the pipeline in a view named META_VIEW.
     * @param aJCas
     */
    override def getNext(aJCas: CAS): Unit = {
      val json = iterator.next().toJson()
      val data = JSONParser.parseDocumentText(json)
      val textToAnalyze = data("title") + " "+ data("intro") + " " + data("text")
      aJCas.setDocumentText(textToAnalyze)
      aJCas.createView("META_VIEW")
      aJCas.getView("META_VIEW").setDocumentText(json)
      mCurrentIndex+=1
    }

    override def hasNext: Boolean = iterator.hasNext

    override def getProgress: Array[Progress] = Array[Progress](new ProgressImpl(mCurrentIndex, docs.size, Progress.ENTITIES))
}