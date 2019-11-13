import java.io.{BufferedInputStream, File, InputStream}
import java.util.Scanner

import com.ibm.icu.text.CharsetDetector
import de.tudarmstadt.ukp.dkpro.core.api.io.ResourceCollectionReaderBase
import de.tudarmstadt.ukp.dkpro.core.api.parameter.ComponentParameters
import de.tudarmstadt.ukp.dkpro.core.api.resources.CompressionUtils
import org.apache.commons.io.IOUtils
import org.apache.uima.cas.CAS
import org.apache.uima.fit.descriptor.ConfigurationParameter
import spray.json._
import DefaultJsonProtocol._

import scala.io.Source

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

    val data = JSONParser.parse(res)
    val text = data("title") + " $$ " + data("intro") + " $$ " + data("article")

    aJCas.setDocumentText(text)
  }
}

object JSONReader {
  final val PARAM_SOURCE_ENCODING = ComponentParameters.PARAM_SOURCE_ENCODING
  final val ENCODING_AUTO = "auto"
}
