package uima

import java.io.{File, FileInputStream, IOException, ObjectInputStream}

import com.typesafe.config.ConfigFactory
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.`type`.Lemma
import org.apache.uima.fit.component.JCasAnnotator_ImplBase
import org.apache.uima.fit.util.JCasUtil
import org.apache.uima.jcas.JCas
import spray.json.DefaultJsonProtocol._
import spray.json._

/**
 * This Component reads the idf model created by the first pipeline,
 * calculates the tf-idf values for all lemmas
 * and extracts the most relevant lemmas (lemmas with the highest tf-idf).
 * The number of most relevant lemmas depends on the total number of lemmas in the document text
 * and can be adapted in the config file.
 */
class TfIdfCalculator extends JCasAnnotator_ImplBase {

  /**
   * Location of the idf model to be read
   */
  val modelPath: String = ConfigFactory.load().getString("app.idfmodellocationread")

  /**
   * Percentage of lemmas of all lemmas to become most relevant lemmas
   */
  val percentOfLemmas: String = ConfigFactory.load().getString("app.percentoflemmas")

  val jsonString: String = deserialize[String](modelPath)
  val jsonAst: JsValue = jsonString.parseJson

  /**
   * The idf model
   */
  val termIdfMap: Map[String, Int] = jsonAst.convertTo[Map[String, Int]]

  /**
   * Calculates tf value for each lemma
   * TF(w) = (Number of times term w appears in a document) / (Total number of terms in the document),
   * calculates tf-idf for each lemma
   * TF-IDF = TF * IDF (taken from idf model)
   * and extracts given percentage of lemmas as most relevant lemmas.
   *
   * @param aJCas
   */
  override def process(aJCas: JCas): Unit = {

    // get all lemmas
    val lemmas = JCasUtil.select(aJCas, classOf[Lemma]).toArray().toList.asInstanceOf[List[Lemma]]

    // total number of lemmas
    val nrOfLemmas = lemmas.size

    // create map with lemmas and their tf-values
    val tfMap = lemmas
      .map(lemma => lemma.getValue)
      .groupBy(x => x)
      .view.mapValues(_.length / nrOfLemmas.toDouble).toMap

    // create and get view for most relevant lemmas
    aJCas.createView("MOST_RELEVANT_VIEW")
    val mostRelevantView = aJCas.getView("MOST_RELEVANT_VIEW")

    // create map with lemmas and their calculated tfIdf-values
    val tfidfMap = tfMap.map(lemma => ( {
      val anno = new Lemma(mostRelevantView)
      anno.setValue(lemma._1)
      anno
    }, lemma._2 * termIdfMap(lemma._1)))

    // get n most relevant (lemmas with highest tfidf values)
    val mostRelevantLemmas = getMostRelevant((nrOfLemmas * percentOfLemmas.toDouble).toInt, tfidfMap)

    // add most relevant lemmas to indexes in the view
    mostRelevantLemmas.foreach(_.addToIndexes())
  }

  /**
   * Sorts the given tf-idf map by descending tf-idf values and returns the first n (amount).
   * @param amount
   * @param tfIdfMap
   * @return List[String]
   */
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



