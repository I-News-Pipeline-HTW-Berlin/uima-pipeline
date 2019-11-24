package uima

import json.JSONParser

import de.tudarmstadt.ukp.dkpro.core.api.io.ResourceCollectionReaderBase
import de.tudarmstadt.ukp.dkpro.core.api.parameter.ComponentParameters
import org.apache.uima.cas.CAS
import org.apache.uima.fit.descriptor.ConfigurationParameter


class JSONReader extends ResourceCollectionReaderBase {

  /**
   * Name of configuration parameter that contains the character encoding used by the input files.
   */
  @ConfigurationParameter(name = JSONReader.PARAM_SOURCE_ENCODING, mandatory = true,
    defaultValue = Array(ComponentParameters.DEFAULT_ENCODING))
  val sourceEncoding = ""

  override def getNext(aJCas: CAS): Unit = {
    val res = nextFile
    initCas(aJCas, res)

    val json = JSONParser.getJsonStringFromResource(res)
    val data = JSONParser.parse(json)
    val text = data("title") + " $$ " + data("intro") + " $$ " + data("article")

    aJCas.setDocumentText(text)
  }
}

object JSONReader {
  final val PARAM_SOURCE_ENCODING = ComponentParameters.PARAM_SOURCE_ENCODING
  final val ENCODING_AUTO = "auto"
}
