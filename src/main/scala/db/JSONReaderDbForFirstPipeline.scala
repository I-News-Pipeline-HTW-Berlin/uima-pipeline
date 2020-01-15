package db

import java.io.File

import db.Helpers._
import de.tudarmstadt.ukp.dkpro.core.api.parameter.ComponentParameters
import json.JSONParser
import org.apache.uima.cas.CAS
import org.apache.uima.fit.component.CasCollectionReader_ImplBase
import org.apache.uima.fit.descriptor.ConfigurationParameter
import org.apache.uima.util.{Progress, ProgressImpl}
import org.mongodb.scala.MongoClient
import org.mongodb.scala.bson.BsonDateTime
import org.mongodb.scala.model.{Filters, Sorts}

import scala.io.Source
//muss noch umbenannt werden
//Der Unterschied zum anderen Reader ist momentan nur, dass nicht in last_crawl_time.txt geschrieben wird. Vielleicht kann man das noch schöner lösen
class JSONReaderDbForFirstPipeline extends CasCollectionReader_ImplBase{

    /**
     * Name of configuration parameter that contains the character encoding used by the input files.
     */
    @ConfigurationParameter(name = JSONReaderDbForFirstPipeline.PARAM_SOURCE_ENCODING, mandatory = true,
      defaultValue = Array(ComponentParameters.DEFAULT_ENCODING))
    val sourceEncoding = ""

    @ConfigurationParameter(name = JSONReaderDbForFirstPipeline.USER_NAME)
    val userName = "inews" //test: s0558059

    @ConfigurationParameter(name = JSONReaderDbForFirstPipeline.PW)
    val pw = "pr3cipit4t3s" //test: f0r313g

    @ConfigurationParameter(name = JSONReaderDbForFirstPipeline.SERVER_ADDRESS)
    val serverAddress = "hadoop05.f4.htw-berlin.de"

    @ConfigurationParameter(name = JSONReaderDbForFirstPipeline.PORT)
    val port = "27020"

    @ConfigurationParameter(name = JSONReaderDbForFirstPipeline.DB)
    val db = "inews" //test: s0558059

    @ConfigurationParameter(name = JSONReaderDbForFirstPipeline.COLLECTION_NAME)
    val collectionName = "scraped_articles" //test: scraped_articles_test

    @ConfigurationParameter(name = JSONReaderDbForFirstPipeline.FILE_LOCATION)
    val fileLocation = "last_crawl_time.txt"

    //val DATE_FORMAT = "EEE, MMM dd, yyyy h:mm a"
    //TODO paar Sachen in Funktionen verpacken
    val mongoClient: MongoClient = DbConnector.createClient(userName, pw, serverAddress, port, db)
    val lastCrawlTime = getLastCrawlTime
    val docs = DbConnector.getCollectionFromDb(db, collectionName, mongoClient)
      .find(Filters.and(Filters.gt("crawl_time", lastCrawlTime), Filters.ne("text", ""),
        Filters.ne("title", null))).sort(Sorts.ascending("crawl_time")).limit(1000).results()
    println(docs.size)
    val it = docs.iterator
    var latestCrawlTime = lastCrawlTime.asDateTime().getValue
    if(!docs.isEmpty){
      latestCrawlTime = docs.map(doc => doc.getOrElse("crawl_time", BsonDateTime(value = 0)))
        .maxBy(date => date.asDateTime().getValue)
        .asDateTime()
        .getValue
    }

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

object JSONReaderDbForFirstPipeline {
  final val PARAM_SOURCE_ENCODING = ComponentParameters.PARAM_SOURCE_ENCODING
  final val ENCODING_AUTO = "auto"
  final val USER_NAME = "inews" // s0558059
  final val PW = "pr3cipit4t3s" // f0r313g
  final val SERVER_ADDRESS = "hadoop05.f4.htw-berlin.de"
  final val PORT = "27020"
  final val DB = "inews" // s0558059
  final val COLLECTION_NAME = "scraped_articles" //scraped_articles_test
  final val FILE_LOCATION = "last_crawl_time.txt"
}

