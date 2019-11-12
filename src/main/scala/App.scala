import org.apache.uima.fit.util.JCasUtil
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.`type`.Lemma

object App {

  def main(args: Array[String]) {
    val corpus = Corpus.fromDir("testResourcesJSON")
    val jcasIterator = corpus.lemmatize()
    jcasIterator.forEachRemaining(jcas => {
      print("\n\n")
      val lemmas = JCasUtil.select(jcas, classOf[Lemma])
      //lemmas.iterator.forEachRemaining(r => print(r.getValue + "\n"))
      print(lemmas)
    })
  }
}