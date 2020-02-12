package uima

import java.io.File

import com.typesafe.config.ConfigFactory
import de.tudarmstadt.ukp.dkpro.core.api.ner.`type`.NamedEntity
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.`type`.Lemma
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD
import org.apache.uima.fit.component.JCasAnnotator_ImplBase
import org.apache.uima.fit.util.JCasUtil
import org.apache.uima.jcas.JCas
import spray.json.DefaultJsonProtocol._
import spray.json._

/**
 * This Component reads the idf model created by the first pipeline, calculates the tf-idf values for all lemmas and
 * named entities and extracts the most relevant lemmas (lemmas and / or named entities with the highest tf-idf. The
 * number of most relevant lemmas depends on the total number of lemmas in the document text and can be adapted in
 * config file.
 */
class TfIdfCalculator extends JCasAnnotator_ImplBase {

  /**
   * Location of the idf model to be read
   */
  val modelPath: String = ConfigFactory.load().getString("app.idfmodellocationread")

  /**
   * Percent of lemmas of all lemmas to become most relevant lemmas
   */
  val percentOfLemmas: String = ConfigFactory.load().getString("app.percentoflemmas")

  val sc: SparkContext = App.getSparkContext

  val modelLines: RDD[String] = RDDFromFile(modelPath)
  val jsonString: String = modelLines.fold("")(_+_)
  val jsonAst: JsValue = jsonString.parseJson

  /**
   * The idf model
   */
  val termIdfMap: Map[String, Double] = jsonAst.convertTo[Map[String, Double]]

  /**
   * Calculates tf value for each lemma
   * TF(w) = (Number of times term w appears in a document) / (Total number of terms in the document)
   * calculates tf-idf for each lemma
   * TF-IDF = TF * IDF (taken from idf model)
   * and extracts given percentage of lemmas as most relevant lemmas.
   * @param aJCas
   */
  override def process(aJCas: JCas): Unit = {

    // get all lemmas and map them to serializable tuples
    val lemmas: List[(Int, Int, String)] = JCasUtil.select(aJCas, classOf[Lemma])
                    .toArray()
                    .toList.asInstanceOf[List[Lemma]]
                    .map(l => (l.getBegin, l.getEnd, l.getValue))

    // get view containing named entities
    val namedEntitiesView: JCas = aJCas.getView("NAMED_ENTITIES_VIEW")

    // get all named entities
    val namedEntities: List[NamedEntity] = JCasUtil.select(namedEntitiesView, classOf[NamedEntity])
                    .toArray
                    .toList.asInstanceOf[List[NamedEntity]]

    val docText = aJCas.getDocumentText

    /* replacing lemmas which describe persons with the corresponding named entity, e.g. lemmas "Elon" and "Musk" are
    being removed from list and will be replaced by "Elon Musk" the value of the named corresponding named entity */
    val lemmasWithNamedEntities: List[(Int, Int, String)] = lemmas.foldLeft(List.empty[(Int, Int, String)])((list, lemma) => {
      val neWithEqualIndex = namedEntities.filter(
        ne => ne.getBegin == lemma._1 || ne.getEnd == lemma._2)
      if(!neWithEqualIndex.isEmpty && lemma._1 == neWithEqualIndex.head.getBegin){
        (neWithEqualIndex.head.getBegin,
          neWithEqualIndex.head.getEnd,
          docText.substring(neWithEqualIndex.head.getBegin, neWithEqualIndex.head.getEnd).toLowerCase)::list
      } else if(!neWithEqualIndex.isEmpty && lemma._2 == neWithEqualIndex.head.getEnd) {
        list
      }
      else {
        lemma::list
      }
    })

    val lemmasWithNamedEntitiesRdd: RDD[(Int, Int, String)] = sc.parallelize(lemmasWithNamedEntities)

    // total number of lemmas after replacing persons with named entities
    val nrOfLemmas: Long = lemmasWithNamedEntitiesRdd.count

    // create rdd with lemmas and their tf-values
    val tfMap: RDD[(String, Double)] = lemmasWithNamedEntitiesRdd
      .map(lemma => lemma._3)
      .groupBy(x => x)
      .mapValues(_.size / nrOfLemmas.toDouble)

    val tfidfMap: RDD[(String, Double)] = TfIdfCalculator.createTfidfMap(tfMap, termIdfMap)

    // create and get view for most relevant lemmas
    aJCas.createView("MOST_RELEVANT_VIEW")
    val mostRelevantView: JCas = aJCas.getView("MOST_RELEVANT_VIEW")

    // get n most relevant (lemmas with highest tfidf values)
    val mostRelevantLemmas: Array[Lemma] = getMostRelevant((nrOfLemmas * percentOfLemmas.toDouble).toInt, tfidfMap)
                      .map(lemma =>  {
                        val anno = new Lemma(mostRelevantView)
                        anno.setValue(lemma)
                        anno})

    // add most relevant lemmas to indexes
    mostRelevantLemmas.foreach(_.addToIndexes())
  }

  /**
   * Sorts the given tf-idf map by descending tf-idf values and returns the first n (amount).
   * @param amount
   * @param tfIdfMap
   * @return Array[String]
   */
  def getMostRelevant(amount: Int, tfIdfMap: RDD[(String, Double)]): Array[String] = {
    tfIdfMap.sortBy(_._2, ascending = false).keys.distinct.take(amount)
  }

  /**
   * Reads the content of text file into an RDD if the file exists, else returns an empty RDD
   * @param path
   * @return RDD[String]
   */
  def RDDFromFile(path: String, isAResource: Boolean = true): RDD[String] = {
    if(!new File(path).exists()){
      println("idf-model file doesnt exist")
      return sc.emptyRDD[String]
    }
    sc.textFile(path)
  }
}

object TfIdfCalculator extends Serializable {

  /**
   * Calculates the tf-idf values
   * @param tfMap
   * @param termIdfMap
   * @return
   */
  def createTfidfMap(tfMap: RDD[(String, Double)], termIdfMap: Map[String, Double] ) : RDD[(String, Double)] = {
    tfMap.map(lemma => (lemma._1, lemma._2 * termIdfMap(lemma._1)))
  }
}


