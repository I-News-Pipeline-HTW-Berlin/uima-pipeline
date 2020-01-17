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

   // @ConfigurationParameter(name = JSONReaderDbForFirstPipeline.USER_NAME)
    val userName = uima.App.user // = ConfigFactory.load().getString("db.user") //"inews" //test: s0558059

   // @ConfigurationParameter(name = JSONReaderDbForFirstPipeline.PW)
    val pw = uima.App.pw //ConfigFactory.load().getString("db.pw")

   // @ConfigurationParameter(name = JSONReaderDbForFirstPipeline.SERVER_ADDRESS)
    val serverAddress = uima.App.server //ConfigFactory.load().getString("db.server") //"hadoop05.f4.htw-berlin.de"

  //  @ConfigurationParameter(name = JSONReaderDbForFirstPipeline.PORT)
    val port = uima.App.port //ConfigFactory.load().getString("db.port") //"27020"

  //  @ConfigurationParameter(name = JSONReaderDbForFirstPipeline.DB)
    val db = uima.App.db //ConfigFactory.load().getString("db.db") //"inews" //test: s0558059

  //  @ConfigurationParameter(name = JSONReaderDbForFirstPipeline.COLLECTION_NAME)
    val collectionName = uima.App.collection //ConfigFactory.load().getString("db.collection") //"scraped_articles" //test: scraped_articles_test

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

 // val configPath = System.getProperty("src/main/resources/application.conf")

 // val config = ConfigFactory.parseFile(new File(configPath))

  final val PARAM_SOURCE_ENCODING = ComponentParameters.PARAM_SOURCE_ENCODING
  final val ENCODING_AUTO = "auto"
  final val USER_NAME = uima.App.user //uima.App.user//ConfigFactory.load().getString("db.user")//config.getString("db.user") // s0558059
  final val PW = uima.App.pw
  final val SERVER_ADDRESS = uima.App.server
  final val PORT = uima.App.port //"27020"
  final val DB = uima.App.db //"inews" // s0558059
  final val COLLECTION_NAME = uima.App.collection //"scraped_articles" //scraped_articles_test
  final val FILE_LOCATION = "last_crawl_time.txt"
}

