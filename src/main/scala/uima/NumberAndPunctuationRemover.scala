package uima

import org.apache.uima.fit.component.JCasAnnotator_ImplBase
import org.apache.uima.fit.util.JCasUtil
import org.apache.uima.jcas.JCas
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.`type`.Lemma
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD

/**
 * The main purpose of this component is to remove numbers and standalone punctuation such as "2600" or ",".
 * It does not remove words that contain numbers or punctuation, such as "i5-prozessor".
 */
class NumberAndPunctuationRemover extends JCasAnnotator_ImplBase{

  val sc: SparkContext = App.getSparkContext

  /**
   * Gets all Lemma annotations, removes them from idexes, cleans the list of standalone numbers and punctuation and readds
   * the remaining lemmas to indexes.
   * @param aJCas
   */
  override def process(aJCas: JCas): Unit = {

    // get all lemmas from indexes
    val lemmas: List[Lemma] = JCasUtil.select(aJCas, classOf[Lemma]).toArray.toList.asInstanceOf[List[Lemma]]

    // remove all lemmas from indexes
    lemmas.foreach(_.removeFromIndexes(aJCas))

    // create tuple instead of lemma to enable serialization
    val lemmasTup: List[(Int, Int, String)] = lemmas.map(l => (l.getBegin, l.getEnd, l.getValue))

    val lemmasRdd: RDD[(Int, Int, String)] = sc.parallelize(lemmasTup)

    // filter lemmas
    val filteredLemmasAsTuple: Array[(Int, Int, String)] = NumberAndPunctuationRemover.filterLemmas(lemmasRdd)

    // recreate Lemma annotations and add them to indexes
    filteredLemmasAsTuple.map(l => {
      val anno = new Lemma(aJCas, l._1, l._2)
      anno.setValue(l._3)
      anno
    }).foreach(_.addToIndexes(aJCas))
  }
}

object NumberAndPunctuationRemover extends Serializable{

  val charactersToRemove: String = "^[0-9,:…;|()-_&—/^‘—‘'*·.’»=«©?“§!„“\"\\\u00AD]+$"

  /**
   * Checks if the given String contains only characters contained in the charactersToRemove array
   * @param s
   * @return Boolean (true if s consists only of characters in charactersToRemove, else false)
   */
  def isAllDigitsOrPunctuation(s: String): Boolean = s.matches(charactersToRemove)

  /**
   * Cleans the given RDD of tuples where the value contains only numbers or punctuation and collects it.
   * @param lemmaRdd
   * @return Array[(Int, Int, String)]
   */
  def filterLemmas(lemmaRdd :RDD[(Int, Int, String)]) : Array[(Int, Int, String)] = {
    lemmaRdd.filter(lemma => !isAllDigitsOrPunctuation(lemma._3)).collect
  }
}
