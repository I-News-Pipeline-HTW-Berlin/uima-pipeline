package db

import org.mongodb.scala.bson.BsonDocument
import org.mongodb.scala.bson.collection.mutable.Document
import org.mongodb.scala.{Completed, MongoClient, MongoCollection, Observer}
import db.Helpers

object DbConnector {

  //TODO Exception Handling??
  def createClient(userName: String = "s0558059",
                   pw: String = "f0r313g",
                   serverAddress: String = "hadoop05.f4.htw-berlin.de",
                   port: String = "27020",
                   db: String = "s0558059"): MongoClient = {
    MongoClient("mongodb://"+userName+":"+pw+"@"+serverAddress+":"+port+"/"+db)
  }

  //TODO Exception Handling??
  def getCollectionFromDb(dbName: String = "s0558059",
                          collectionName: String = "scraped_articles",
                          mongoClient: MongoClient = createClient()): MongoCollection[Document] = {
    mongoClient.getDatabase(dbName).getCollection(collectionName)
  }

  def writeSingleDocumentToCollection(collection: MongoCollection[Document] = getCollectionFromDb(),
                                      docJsonString: String) = {
    val doc = Document(docJsonString)
    collection.insertOne(doc).subscribe(new Observer[Completed] {
      override def onNext(result: Completed): Unit = println("inserted")
      override def onError(e: Throwable): Unit = print(e.getStackTrace)
      override def onComplete(): Unit = println("completed")
    })
  }

  def writeMultipleDocumentsToCollection(collection: MongoCollection[Document] = getCollectionFromDb(),
                                         jsonStringList: List[String]) = {
    val docs = jsonStringList.map(j => Document(BsonDocument(j)))
    //documentList.map(doc => collection.insertOne(doc))
    print("huhu !")
    /*collection.insertMany(docs).subscribe(new Observer[Completed] {
      override def onNext(result: Completed): Unit = println("inserted")
      override def onError(e: Throwable): Unit = println(e.printStackTrace())
      override def onComplete(): Unit = println("completed")
    })*/
    val insertObservable = collection.insertMany(docs)

    val insertAndCount = for {
      insertResult <- insertObservable
      countResult <- collection.estimatedDocumentCount()
    } yield countResult

    //println(s"total # of documents after inserting 100 small ones (should be 101):  ${insertAndCount.headResult}")
  }
}
