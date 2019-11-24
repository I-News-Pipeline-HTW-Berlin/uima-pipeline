package uima

import db.DbConnector
import db.Helpers._
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.`type`.Lemma
import org.apache.uima.fit.util.JCasUtil

object App {

  def main(args: Array[String]) {
    //val corpus = Corpus.fromDir("testResourcesJSON")
    val corpus = Corpus.fromDb()
    val jcasIterator = corpus.lemmatize()
    //val jcasIterator = corpus.tokenize()
    //val jcasIterator = corpus.estimateReadingTime()
    jcasIterator.forEachRemaining(jcas => {
      print("\n\n")
      val lemmas = JCasUtil.select(jcas, classOf[Lemma])
      //val tokens = JCasUtil.select(jcas, classOf[Token])
      //val readingTimes = JCasUtil.select(jcas, classOf[Token])
      lemmas.iterator.forEachRemaining(r => print(r.getValue + "\n"))
      //print(lemmas)
      //print(tokens)
      //tokens.iterator.forEachRemaining(t => print(t.getText + "\n"))
      //readingTimes.iterator().forEachRemaining(rt => print(rt.getForm.getCoveredText))
    })


    //val collection = DbConnector.getCollectionFromDb()
    //collection.find().first().printHeadResult()
    /*val results = collection.find().toFuture()
    implicit val ec: scala.concurrent.ExecutionContext = scala.concurrent.ExecutionContext.global
    results.foreach(res => println(res.toString()))*/



  }
}
