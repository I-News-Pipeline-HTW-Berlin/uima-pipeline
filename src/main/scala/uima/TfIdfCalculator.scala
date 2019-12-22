package uima

import java.io.{File, FileInputStream, IOException, ObjectInputStream}

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.`type`.Lemma
import org.apache.uima.fit.component.JCasAnnotator_ImplBase
import org.apache.uima.fit.descriptor.ConfigurationParameter
import org.apache.uima.fit.util.JCasUtil
import org.apache.uima.jcas.JCas
import spray.json._
import DefaultJsonProtocol._

class TfIdfCalculator extends JCasAnnotator_ImplBase {

  @ConfigurationParameter(name = TfIdfCalculator.MODEL_PATH)
  val modelPath = "src/main/resources/tfidfmodel.json"

  @ConfigurationParameter(name = TfIdfCalculator.N_MOST_RELEVANT)
  val nMostRelevant = "10"


  // deserialize tfidfmodel.json and read in as map
  var termIdfMap = Map.empty[String, Int]
  val jsonString: String = deserialize[String](modelPath)
  val jsonAst: JsValue = jsonString.parseJson
  termIdfMap = jsonAst.convertTo[Map[String, Int]]

  // calculate TF(w) = (Number of times term w appears in a document) / (Total number of terms in the document)
  override def process(aJCas: JCas): Unit = {

    val lemmas = JCasUtil.select(aJCas, classOf[Lemma]).toArray().toList

    // create map with lemmas and their tf-values
    val tfMap = lemmas
      .map(lemma => lemma.asInstanceOf[Lemma].getValue)
      .groupBy(x => x)
      .view.mapValues(_.length / lemmas.size.toDouble).toMap

    // create map with lemmas and their calculated tfIdf-values
    val tfidfMap = tfMap.map(lemma => (lemma._1, lemma._2 * termIdfMap(lemma._1)))
    println(tfidfMap)

    // get n most relevant (lemmas with highest tfidf values)
    val mostRelevantLemmas = getMostRelevant(nMostRelevant, tfidfMap)
    println(mostRelevantLemmas)
  }

  def getMostRelevant(amount: String, tfIdfMap: Map[String, Double]): List[String] = {
    List(tfIdfMap.toSeq.sortWith(_._2 > _._2):_*).take(amount.toInt).toMap.keys.toList
  }

  @SuppressWarnings(Array("unchecked"))
  @throws[IOException]
  def deserialize[T](filePath: String): T = try {
    val in = new ObjectInputStream(new FileInputStream(new File(filePath)))
    try in.readObject.asInstanceOf[T]
    catch {
      case e: ClassNotFoundException =>
        throw new IOException(e)
    } finally if (in != null) in.close()
  }

}

  object TfIdfCalculator {
    final val MODEL_PATH = "src/main/resources/tfidfmodel.json"
    final val N_MOST_RELEVANT = "10"
  }


