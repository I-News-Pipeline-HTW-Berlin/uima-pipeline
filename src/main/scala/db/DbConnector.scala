package db

import org.mongodb.scala.{Completed, Document, MongoClient, MongoCollection, Observer}


object DbConnector {

  /**
   * Creates Client for MongoDB
   * @param userName
   * @param pw
   * @param serverAddress
   * @param port
   * @param db
   * @return MongoClient
   */
  def createClient(userName: String,
                   pw: String,
                   serverAddress: String,
                   port: String,
                   db: String): MongoClient = {
    MongoClient("mongodb://"+userName+":"+pw+"@"+serverAddress+":"+port+"/"+db)
  }

  /**
   * Returns MongoDB collection
   * @param dbName
   * @param collectionName
   * @param mongoClient
   * @return MongoCollection[Document]
   */
  def getCollectionFromDb(dbName: String,
                          collectionName: String,
                          mongoClient: MongoClient): MongoCollection[Document] = {
    mongoClient.getDatabase(dbName).getCollection(collectionName)
  }

  /**
   * Creates a Document from json String and writes it to db
   * @param collection
   * @param docJsonString
   */
  def writeSingleDocumentToCollection(collection: MongoCollection[Document],
                                      docJsonString: String): Unit = {
    val doc = Document(docJsonString)
    collection.insertOne(doc).subscribe(new Observer[Completed] {
      override def onNext(result: Completed): Unit = {}
      override def onError(e: Throwable): Unit = e.printStackTrace()
      override def onComplete(): Unit = {}
    })
  }

}
