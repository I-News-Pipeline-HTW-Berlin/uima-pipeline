package db

import com.mongodb.DuplicateKeyException
import org.mongodb.scala.bson.BsonDocument
import org.mongodb.scala.bson.collection.mutable.Document
import org.mongodb.scala.{Completed, MongoClient, MongoCollection, Observer}
import db.Helpers._

object DbConnector {

  //TODO Exception Handling??
  def createClient(userName: String,
                   pw: String,
                   serverAddress: String,
                   port: String,
                   db: String): MongoClient = {
    MongoClient("mongodb://"+userName+":"+pw+"@"+serverAddress+":"+port+"/"+db)
  }

  //TODO Exception Handling??
  def getCollectionFromDb(dbName: String,
                          collectionName: String,
                          mongoClient: MongoClient): MongoCollection[Document] = {
    mongoClient.getDatabase(dbName).getCollection(collectionName)
  }

  def writeSingleDocumentToCollection(collection: MongoCollection[Document],
                                      docJsonString: String) = {
    val doc = Document(docJsonString)
    collection.insertOne(doc).subscribe(new Observer[Completed] {
      override def onNext(result: Completed): Unit = println("inserted")
      override def onError(e: Throwable): Unit = print(e.getStackTrace)
      override def onComplete(): Unit = println("completed")
    })
  }

  def writeMultipleDocumentsToCollection(collection: MongoCollection[Document],
                                         jsonStringList: IndexedSeq[String]): Unit = {
    //println("Anzahl Dokumente " + collection.countDocuments().headResult())
    //jsonStringList.map(j => collection.insertOne(Document(j)))
    //jsonStringList.map(j => writeSingleDocumentToCollection(collection, j))
    //documentList.map(doc => collection.insertOne(doc))

    val docs = jsonStringList.map(json => json match {
      case null => Document(BsonDocument())
      case _ => Document(BsonDocument(json))
    })

    /*val insertObservable = collection.insertMany(docs)
    insertObservable.subscribe(new Observer[Completed] {
      override def onNext(result: Completed): Unit = println("inserted")
      override def onError(e: Throwable): Unit = println(e.printStackTrace())
      override def onComplete(): Unit = println("completed")
    })*/

    //Await.ready(collection.insertMany(jsonStringList.map(json => Document(json))).toFuture(), 20.seconds)
    try {
      //TODO wie kann das schöner werden?
      val insertObservable = collection.insertMany(docs)

      val insertAndCount = for {
        insertResult <- insertObservable
        countResult <- collection.estimatedDocumentCount()
      } yield countResult

      println(s"total # of documents after inserting 100 small ones (should be 101):  ${insertAndCount.headResult()}")
    } catch {
          //TODO write something into log file
        case dke: DuplicateKeyException => println("One or more ids already exist. "+dke.getMessage)
        case e: Exception => e.printStackTrace()
    }

  }
}
