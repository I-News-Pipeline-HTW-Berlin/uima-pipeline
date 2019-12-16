package uima

import db.DbConnector
import db.Helpers._
import de.tudarmstadt.ukp.dkpro.core.api.metadata.`type`.MetaDataStringField
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.`type`.{Lemma, Token}
import org.apache.uima.fit.util.JCasUtil
import org.mongodb.scala.MongoCollection
import org.mongodb.scala.bson.collection.mutable.Document

//oder mit home/laureslinuxes/ noch vorne ran
//zum testen datei hier abgelegt: ~/Dokumente/Uni/WiSe19-20/Projektstudium/uima_resources
//für server aber: /home/uima/resources/last_crawl_time.txt
object App {

  def main(args: Array[String]) {
    //val corpus = Corpus.fromDir("testResourcesJSON")
    val corpus = Corpus.fromDb("s0558059", "f0r313g", "hadoop05.f4.htw-berlin.de",
      "27020", "s0558059", "scraped_articles", "../../../uima_resources/last_crawl_time.txt")
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
      val mongoClient = DbConnector.createClient("s0558478", "1unch30n", "hadoop05.f4.htw-berlin.de", "27020", "s0558478")
      val collection = DbConnector.getCollectionFromDb("s0558478", "processed_articles", mongoClient)
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
