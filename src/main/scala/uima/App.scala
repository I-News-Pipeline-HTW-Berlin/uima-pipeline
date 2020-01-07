package uima

import db.DbConnector
import de.tudarmstadt.ukp.dkpro.core.api.frequency.tfidf.`type`.Tfidf
import de.tudarmstadt.ukp.dkpro.core.api.metadata.`type`.MetaDataStringField
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.`type`.Lemma
import org.apache.uima.fit.util.JCasUtil
import org.josql.parser.Token

object App {

  def main(args: Array[String]) {
    //val corpus = Corpus.fromDir("testResourcesJSON")

    //ZUM TESTEN IN FLIPS DB:
    /*val corpus = Corpus.fromDb("s0558059", "f0r313g", "hadoop05.f4.htw-berlin.de",
      "27020", "s0558059", "scraped_articles", "last_crawl_time.txt")*/

    //ZUM TESTEN, OB ES FUNZT BEI MEHRMALIGEN DURCHLAUF:
    val corpus = Corpus.fromDb("s0558059", "f0r313g", "hadoop05.f4.htw-berlin.de",
      "27020", "s0558059", "test_scraped_articles_sorted", "last_crawl_time.txt")

    //AUF INEWS SERVER LAUFEN LASSEN:
    /*val corpus = Corpus.fromDb("inews", "pr3cipit4t3s", "hadoop05.f4.htw-berlin.de",
      "27020", "inews", "scraped_articles", "last_crawl_time.txt")*/

    //val jcasIteratorLemmas = corpus.lemmatize()
    //val jcasIterator = corpus.tokenize()
    //val jcasIteratorRT = corpus.estimateReadingTime()
    /*jcasIteratorLemmas.forEachRemaining(jcas => {
      print("\n\n")
      val lemmas = JCasUtil.select(jcas, classOf[Lemma])
      //val tokens = JCasUtil.select(jcas, classOf[Token])
      //val readingTimes = JCasUtil.select(jcas, classOf[MetaDataStringField])
      lemmas.iterator.forEachRemaining(r => print(r.getValue + "\n"))
      //print(lemmas)
      //print(tokens)
      //tokens.iterator.forEachRemaining(t => print(t.getText + "\n"))
      //readingTimes.iterator().forEachRemaining(rt => print(rt.getKey+": "+rt.getValue))
    })
    jcasIteratorRT.forEachRemaining(jcas => {
      print("\n\n")
      //val lemmas = JCasUtil.select(jcas, classOf[Lemma])
      //val tokens = JCasUtil.select(jcas, classOf[Token])
      val readingTimes = JCasUtil.select(jcas, classOf[MetaDataStringField])
      //lemmas.iterator.forEachRemaining(r => print(r.getValue + "\n"))
      //print(lemmas)
      //print(tokens)
      //tokens.iterator.forEachRemaining(t => print(t.getText + "\n"))
      readingTimes.iterator().forEachRemaining(rt => print(rt.getKey+": "+rt.getValue))
    })*/

    // nachdem dfmodel.model erstellt wurde, diese Zeilen einkommentieren und ausführen:

    /*val testPipeIt = corpus.testPipeline()
    testPipeIt.forEachRemaining(jcas => {
      val tfidfs = JCasUtil.select(jcas, classOf[Tfidf])
      val lemmas = JCasUtil.select(jcas, classOf[Lemma])
      println("new document:")
      lemmas.iterator().forEachRemaining(lemma => println(lemma.getValue))
      tfidfs.iterator().forEachRemaining(tfidf => println(tfidf.getTerm + ", tfidfwert: " + tfidf.getTfidfValue))
      println()
    })*/

    /**
     * erste Pipeline (mit IDF):
     */
    val modelIt = corpus.writeModel()
    modelIt.forEachRemaining(jcas => {
      val lemmas = JCasUtil.select(jcas, classOf[Lemma])
      /*println("most relevant in this article:")
      lemmas.forEach(l => println(l))
      println()*/
    })

    /**
     * 2. Pipeline (mit TF-IDF):
     */
    val testPipeIt = corpus.testPipeline()
    //val mc : MongoCollection[Document] = new MongoCollection[Document]()
    //TODO make it nice
    var jsonList : IndexedSeq[String] = IndexedSeq.empty
    testPipeIt.forEachRemaining(jcas => {
      val json =JCasUtil.select(jcas, classOf[MetaDataStringField]).toArray.toList.head.asInstanceOf[MetaDataStringField].getValue
      //jsonList.foldLeft(List.empty)((l, j) => l:+j)
      //jsonList = json::jsonList
      jsonList = json+:jsonList
    })
    //println("Länge der Liste: "+ jsonList.size)
    //Exception abfangen, falls Liste empty
    if(!jsonList.isEmpty){
      val mongoClient = DbConnector.createClient("s0558059", "f0r313g", "hadoop05.f4.htw-berlin.de", "27020", "s0558059")
      val collection = DbConnector.getCollectionFromDb("s0558059", "test_processed_articles", mongoClient)
      //jsonList.map(doc => collection.insertOne(Document(doc)))

      DbConnector.writeMultipleDocumentsToCollection(collection, jsonList)
    } else {
      //TODO things like that should be written to log file
      println("Currently no documents to analyze. ")
    }


    //val collection = DbConnector.getCollectionFromDb()
    //collection.find().first().printHeadResult()
    /*val results = collection.find().toFuture()
    implicit val ec: scala.concurrent.ExecutionContext = scala.concurrent.ExecutionContext.global
    results.foreach(res => println(res.toString()))*/



  }
}
