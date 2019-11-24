package db

import com.mongodb.{ConnectionString, MongoCredential}
import org.mongodb.scala.bson.collection.mutable.Document
import org.mongodb.scala.{Completed, FindObservable, MongoClient, MongoClientSettings, MongoCollection, MongoDatabase, Observable, Observer, ReadPreference, ServerAddress}
import org.mongodb.scala.connection.ClusterSettings
import com.mongodb.MongoCredential._
import java.util.logging.{Level, Logger}

import scala.jdk.CollectionConverters
import scala.collection.JavaConverters._
import java.util.concurrent.CountDownLatch

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

  /*def connectToDB: MongoCollection[Document] = {

    val mongoClient: MongoClient = MongoClient("mongodb://s0558059:f0r313g@hadoop05.f4.htw-berlin.de:27020/s0558059")
    val db: MongoDatabase = mongoClient.getDatabase("s0558059")
    db.getCollection("scraped_articles")

  }*/
}
