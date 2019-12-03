package uima

import db.DbConnector
import db.Helpers._
import de.tudarmstadt.ukp.dkpro.core.api.metadata.`type`.MetaDataStringField
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.`type`.{Lemma, Token}
import org.apache.uima.fit.util.JCasUtil
import org.mongodb.scala.MongoCollection
import org.mongodb.scala.bson.collection.mutable.Document

object App {

  def main(args: Array[String]) {
    //val corpus = Corpus.fromDir("testResourcesJSON")
    val corpus = Corpus.fromDb("s0558059", "f0r313g", "hadoop05.f4.htw-berlin.de",
      "27020", "s0558059", "scraped_articles", "src/main/resources/last_crawl_time.txt")
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
    var jsonList : List[String] = List.empty
    testPipeIt.forEachRemaining(jcas => {
      val json =JCasUtil.select(jcas, classOf[MetaDataStringField]).toArray.toList.head.asInstanceOf[MetaDataStringField].getValue
      //jsonList.foldLeft(List.empty)((l, j) => l:+j)
      jsonList = json::jsonList
    })
    println("Länge der Liste: "+ jsonList.size)
    //Exception abfangen, falls Liste empty
    val mongoClient = DbConnector.createClient("s0558478", "1unch30n", "hadoop05.f4.htw-berlin.de", "27020", "s0558478")
    val collection = DbConnector.getCollectionFromDb("s0558478", "processed_articles", mongoClient)
    //documentList.map(doc => collection.insertOne(doc))

    DbConnector.writeMultipleDocumentsToCollection(collection, jsonList)

    /*
      object JsonWriter {
    final val USER_NAME = "s0558478"
    final val PW = "1unch30n"
    final val SERVER_ADDRESS = "hadoop05.f4.htw-berlin.de"
    final val PORT = "27020"
    final val DB = "s0558478"
    final val COLLECTION_NAME = "processed_articles"
     */
    //val collection = DbConnector.getCollectionFromDb()
    //collection.find().first().printHeadResult()
    /*val results = collection.find().toFuture()
    implicit val ec: scala.concurrent.ExecutionContext = scala.concurrent.ExecutionContext.global
    results.foreach(res => println(res.toString()))*/



  }
}
