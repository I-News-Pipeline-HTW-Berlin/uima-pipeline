package uima

import org.apache.uima.fit.component.JCasAnnotator_ImplBase
import org.apache.uima.fit.util.JCasUtil
import org.apache.uima.jcas.JCas
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.`type`.{Lemma, Token}
import org.apache.spark.rdd.RDD

class NumberAndPunctuationRemover extends JCasAnnotator_ImplBase{


  val sc = App.getSparkContext

  override def process(aJCas: JCas): Unit = {
    val lemmas = JCasUtil.select(aJCas, classOf[Lemma]).toArray.toList.asInstanceOf[List[Lemma]]

    lemmas.foreach(_.removeFromIndexes(aJCas))

    val lemmasTup = lemmas.map(l => (l.getBegin, l.getEnd, l.getValue))


    val lemmasRdd = sc.parallelize(lemmasTup)
    val filteredLemmasAsTuple = NumberAndPunctuationRemover.filterLemmas(lemmasRdd)



    filteredLemmasAsTuple.map(l => {
      val anno = new Lemma(aJCas, l._1, l._2)
      anno.setValue(l._3)
      anno
    }).foreach(_.addToIndexes(aJCas))


  }
}

object NumberAndPunctuationRemover extends Serializable{

  val charactersToRemove = "^[0-9,:…;|()-_&—/^‘—‘'*·.’»=«©?“§!„“\"\\\u00AD]+$"

  def isAllDigitsOrPunctuation(x: String) = x.matches(charactersToRemove)

  def filterLemmas(lemmaRdd :RDD[(Int, Int, String)]) : Array[(Int, Int, String)] = {
    lemmaRdd.filter(lemma => {
      if(isAllDigitsOrPunctuation(lemma._3)) false
      else true
    }).collect
  }
}
