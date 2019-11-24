package db

import db.Helpers._
import de.tudarmstadt.ukp.dkpro.core.api.io.ResourceCollectionReaderBase
import de.tudarmstadt.ukp.dkpro.core.api.parameter.ComponentParameters
import json.JSONParser
import org.apache.uima.cas.CAS
import org.apache.uima.fit.component.CasCollectionReader_ImplBase
import org.apache.uima.fit.descriptor.ConfigurationParameter
import org.apache.uima.util.Progress

import org.apache.uima.util.ProgressImpl

class JSONReaderDB extends CasCollectionReader_ImplBase {

  /**
   * Name of configuration parameter that contains the character encoding used by the input files.
   */
  @ConfigurationParameter(name = JSONReader.PARAM_SOURCE_ENCODING, mandatory = true,
    defaultValue = Array(ComponentParameters.DEFAULT_ENCODING))
  val sourceEncoding = ""
  val docs = DbConnector.getCollectionFromDb().find().results()
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

object JSONReader {
  final val PARAM_SOURCE_ENCODING = ComponentParameters.PARAM_SOURCE_ENCODING
  final val ENCODING_AUTO = "auto"
}
