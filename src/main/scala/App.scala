import org.apache.uima.fit.util.JCasUtil
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.`type`.{Lemma, Token}
import db.DbConnector

object App {

  def main(args: Array[String]) {
    /*val corpus = Corpus.fromDir("testResourcesJSON")
    //val jcasIterator = corpus.lemmatize()
    //val jcasIterator = corpus.tokenize()
    val jcasIterator = corpus.estimateReadingTime()
    jcasIterator.forEachRemaining(jcas => {
      print("\n\n")
      //val lemmas = JCasUtil.select(jcas, classOf[Lemma])
      //val tokens = JCasUtil.select(jcas, classOf[Token])
      val readingTimes = JCasUtil.select(jcas, classOf[Token])
      //lemmas.iterator.forEachRemaining(r => print(r.getValue + "\n"))
      //print(lemmas)
      //print(tokens)
      //tokens.iterator.forEachRemaining(t => print(t.getText + "\n"))
      readingTimes.iterator().forEachRemaining(rt => print(rt.getForm.getCoveredText))
    })*/

    val collection = DbConnector.connectToDB
    val ob = collection.find()
    print(ob.first().head())

  }
}