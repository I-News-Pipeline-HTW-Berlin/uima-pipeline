package uima

import java.io.{File, FileInputStream, IOException, ObjectInputStream}

import de.tudarmstadt.ukp.dkpro.core.api.ner.`type`.NamedEntity
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.`type`.Lemma
import org.apache.uima.fit.component.JCasAnnotator_ImplBase
import org.apache.uima.fit.descriptor.ConfigurationParameter
import org.apache.uima.fit.util.JCasUtil
import org.apache.uima.jcas.JCas
import spray.json.DefaultJsonProtocol._
import spray.json._

class TfIdfCalculator extends JCasAnnotator_ImplBase {

  @ConfigurationParameter(name = TfIdfCalculator.MODEL_PATH)
  val modelPath = "src/main/resources/idfmodel.json"

  @ConfigurationParameter(name = TfIdfCalculator.PERCENT_OF_LEMMAS)
  val percentOfLemmas = "0.0285"


  // deserialize tfidfmodel.json and read in as map
  val jsonString: String = deserialize[String](modelPath)
  val jsonAst: JsValue = jsonString.parseJson
  val termIdfMap = jsonAst.convertTo[Map[String, Int]]

  // calculate TF(w) = (Number of times term w appears in a document) / (Total number of terms in the document)
  override def process(aJCas: JCas): Unit = {

    val lemmas = JCasUtil.select(aJCas, classOf[Lemma]).toArray().toList.asInstanceOf[List[Lemma]]
    val namedEntitiesView = aJCas.getView("NAMED_ENTITIES_VIEW")
    val namedEntities = JCasUtil.select(namedEntitiesView, classOf[NamedEntity]).toArray.toList.asInstanceOf[List[NamedEntity]]
    val docText = aJCas.getDocumentText

    //hier werden lemmas wie "Elon" und "Musk" ersetzt durch "Elon Musk"
    // erstmal nur für personen, TODO: überlegen, was und ob wir mit den restlichen namedEntities machen
    val lemmasWithNamedEntities = lemmas.foldLeft(List.empty[Lemma])((list, lemma) => {
      val neWithEqualIndex = namedEntities.filter(
        ne => ne.getValue.equalsIgnoreCase("person") && (ne.getBegin == lemma.getBegin || ne.getEnd == lemma.getEnd))
      if(!neWithEqualIndex.isEmpty && lemma.getBegin == neWithEqualIndex.head.getBegin){
        val newLem = new Lemma(aJCas, neWithEqualIndex.head.getBegin, neWithEqualIndex.head.getEnd)
        newLem.setValue(docText.substring(neWithEqualIndex.head.getBegin, neWithEqualIndex.head.getEnd).toLowerCase)
        newLem::list
      } else if(!neWithEqualIndex.isEmpty && lemma.getEnd == neWithEqualIndex.head.getEnd) {
        list
      }
      else {
        lemma::list
      }
    })
    val nrOfLemmas = lemmasWithNamedEntities.size

    // create map with lemmas and their tf-values
    val tfMap = lemmasWithNamedEntities
      .map(lemma => lemma.asInstanceOf[Lemma].getValue)
      .groupBy(x => x)
      .view.mapValues(_.length / nrOfLemmas.toDouble).toMap

    // create map with lemmas and their calculated tfIdf-values
    aJCas.createView("MOST_RELEVANT_VIEW")
    val mostRelevantView = aJCas.getView("MOST_RELEVANT_VIEW")
    val tfidfMap = tfMap.map(lemma => ( {
      val anno = new Lemma(mostRelevantView)
      anno.setValue(lemma._1)
      anno
    }, lemma._2 * termIdfMap(lemma._1)))

    // get n most relevant (lemmas with highest tfidf values)
    val mostRelevantLemmas = getMostRelevant((nrOfLemmas * percentOfLemmas.toDouble).toInt, tfidfMap)
    mostRelevantLemmas.foreach(_.addToIndexes())
  }

  def getMostRelevant(amount: Int, tfIdfMap: Map[Lemma, Double]): List[Lemma] = {
    List(tfIdfMap.toSeq.sortWith(_._2 > _._2): _*).take(amount).toMap.keys.toList
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
  final val MODEL_PATH = "src/main/resources/idfmodel.json"
  final val PERCENT_OF_LEMMAS = "0.0285"
}


