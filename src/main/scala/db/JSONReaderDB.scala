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

class JSONReaderDB extends CasCollectionReader_ImplBase {

  /**
   * Name of configuration parameter that contains the character encoding used by the input files.
   */
  @ConfigurationParameter(name = JSONReaderDB.PARAM_SOURCE_ENCODING, mandatory = true,
    defaultValue = Array(ComponentParameters.DEFAULT_ENCODING))
  val sourceEncoding = ""

  @ConfigurationParameter(name = JSONReaderDB.USER_NAME)
  val userName = "s0558059"

  @ConfigurationParameter(name = JSONReaderDB.PW)
  val pw = "f0r313g"

  @ConfigurationParameter(name = JSONReaderDB.SERVER_ADDRESS)
  val serverAddress = "hadoop05.f4.htw-berlin.de"

  @ConfigurationParameter(name = JSONReaderDB.PORT)
  val port = "27020"

  @ConfigurationParameter(name = JSONReaderDB.DB)
  val db = "s0558059"

  @ConfigurationParameter(name = JSONReaderDB.COLLECTION_NAME)
  val collectionName = "scraped_articles"

  val mongoClient: MongoClient = DbConnector.createClient(userName, pw, serverAddress, port, db)
  val docs = DbConnector.getCollectionFromDb(db, collectionName, mongoClient).find().results()
  val it = docs.iterator
  var mCurrentIndex: Int = 0


  //TODO Exception Handling
  override def getNext(aJCas: CAS): Unit = {
      val json = it.next().toJson()
      val data = JSONParser.parse(json)
      val textToAnalyze = data("title") + " $$ " + data("description")+ " $$ " + data("intro") + " $$ " + data("text")
      aJCas.setDocumentText(textToAnalyze)
      mCurrentIndex+=1
  }

  override def hasNext: Boolean = it.hasNext

  override def getProgress: Array[Progress] = Array[Progress](new ProgressImpl(mCurrentIndex, docs.size, Progress.ENTITIES))
}

object JSONReaderDB {
  final val PARAM_SOURCE_ENCODING = ComponentParameters.PARAM_SOURCE_ENCODING
  final val ENCODING_AUTO = "auto"
  final val USER_NAME = "s0558059"
  final val PW = "f0r313g"
  final val SERVER_ADDRESS = "hadoop05.f4.htw-berlin.de"
  final val PORT = "27020"
  final val DB = "s0558059"
  final val COLLECTION_NAME = "scraped_articles"
}
