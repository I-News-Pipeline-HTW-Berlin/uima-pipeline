package db

import db.Helpers._
import de.tudarmstadt.ukp.dkpro.core.api.parameter.ComponentParameters
import json.JSONParser
import org.apache.uima.cas.CAS
import org.apache.uima.fit.component.CasCollectionReader_ImplBase
import org.apache.uima.fit.descriptor.ConfigurationParameter
import org.apache.uima.util.Progress
import org.apache.uima.util.ProgressImpl
import org.mongodb.scala.MongoClient
import org.mongodb.scala.model.Filters
import java.io.{File, PrintWriter}

import org.mongodb.scala.bson.BsonDateTime

import scala.io.Source

class JSONReaderDB extends CasCollectionReader_ImplBase {

  /**
   * Name of configuration parameter that contains the character encoding used by the input files.
   */
  @ConfigurationParameter(name = JSONReaderDB.PARAM_SOURCE_ENCODING, mandatory = true,
    defaultValue = Array(ComponentParameters.DEFAULT_ENCODING))
  val sourceEncoding = ""

  @ConfigurationParameter(name = JSONReaderDB.USER_NAME)
  val userName = "s0558059" //eig: inews

  @ConfigurationParameter(name = JSONReaderDB.PW)
  val pw = "f0r313g" //eig: pr3cipit4t3s

  @ConfigurationParameter(name = JSONReaderDB.SERVER_ADDRESS)
  val serverAddress = "hadoop05.f4.htw-berlin.de"

  @ConfigurationParameter(name = JSONReaderDB.PORT)
  val port = "27020"

  @ConfigurationParameter(name = JSONReaderDB.DB)
  val db = "s0558059" //eig: inews

  @ConfigurationParameter(name = JSONReaderDB.COLLECTION_NAME)
  val collectionName = "scraped_articles"

  @ConfigurationParameter(name = JSONReaderDB.FILE_LOCATION)
  val fileLocation = "last_crawl_time.txt"

  //val DATE_FORMAT = "EEE, MMM dd, yyyy h:mm a"
  //TODO paar Sachen in Funktionen verpacken
  val mongoClient: MongoClient = DbConnector.createClient(userName, pw, serverAddress, port, db)
  val lastCrawlTime = getLastCrawlTime
  val docs = DbConnector.getCollectionFromDb(db, collectionName, mongoClient)
    .find(Filters.and(Filters.gt("crawl_time", lastCrawlTime), Filters.ne("text", ""),
      Filters.ne("title", null))).results()
  println(docs.size)
  val it = docs.iterator
  var latestCrawlTime = lastCrawlTime.asDateTime().getValue
  if(!docs.isEmpty){
    latestCrawlTime = docs.map(doc => doc.getOrElse("crawl_time", BsonDateTime(value = 0)))
      .maxBy(date => date.asDateTime().getValue)
      .asDateTime()
      .getValue
  }
  writeLastCrawlTimeToDateFile(latestCrawlTime.toString)
  var mCurrentIndex: Int = 0

  def getLastCrawlTime: BsonDateTime = {
    //val dateFormat = new SimpleDateFormat(DATE_FORMAT)
    if (new File(fileLocation).exists) {
      val fileSource = Source.fromFile(fileLocation)
      if (fileSource.hasNext) {
        val dateAsString = fileSource.getLines().next()
        return new BsonDateTime(dateAsString.toLong)
      }
    }
    new BsonDateTime(0)
  }

  def writeLastCrawlTimeToDateFile(latestCrawlTime: String): Unit = {
    val file = new File(fileLocation)
    val printWriter = new PrintWriter(file)
    printWriter.write(latestCrawlTime)
    printWriter.close()
  }


  //TODO Exception Handling
  override def getNext(aJCas: CAS): Unit = {
      val json = it.next().toJson()
      val data = JSONParser.parseStrings(json)
      // Trenner erstmal raus, werden aber eventuell wieder benötigt
      //val textToAnalyze = data("title") + " $$ " + data("description")+ " $$ " + data("intro") + " $$ " + data("text")
      val textToAnalyze = data("title") + " "+ data("intro") + " " + data("text")
      aJCas.setDocumentText(textToAnalyze)
      aJCas.createView("META_VIEW")
      aJCas.getView("META_VIEW").setDocumentText(json)
      aJCas.createView("SIZE_VIEW")
      aJCas.getView("SIZE_VIEW").setDocumentText(docs.size.toString)
      mCurrentIndex+=1
  }

  override def hasNext: Boolean = it.hasNext

  override def getProgress: Array[Progress] = Array[Progress](new ProgressImpl(mCurrentIndex, docs.size, Progress.ENTITIES))
}

object JSONReaderDB {
  final val PARAM_SOURCE_ENCODING = ComponentParameters.PARAM_SOURCE_ENCODING
  final val ENCODING_AUTO = "auto"
  final val USER_NAME = "s0558059" //inews
  final val PW = "f0r313g" //pr3cipit4t3s
  final val SERVER_ADDRESS = "hadoop05.f4.htw-berlin.de"
  final val PORT = "27020"
  final val DB = "s0558059" //inews
  final val COLLECTION_NAME = "scraped_articles"
  final val FILE_LOCATION = "last_crawl_time.txt"
}
