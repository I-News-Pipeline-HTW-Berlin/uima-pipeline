package db

import com.mongodb.MongoCredential
import org.mongodb.scala.bson.collection.mutable.Document
import org.mongodb.scala.{Completed, FindObservable, MongoClient, MongoClientSettings, MongoCollection, MongoDatabase, Observable,Observer, ReadPreference, ServerAddress}
import org.mongodb.scala.connection.ClusterSettings
import com.mongodb.MongoCredential._
import java.util.logging.{Level, Logger}
import org.mongodb.scala.connection.{NettyStreamFactoryFactory,SslSettings}
import scala.jdk.CollectionConverters
import scala.collection.JavaConverters._

object DbConnector {

  def connectToDB: MongoCollection[Document] = {

    val client:MongoClient=MongoClient("mongodb://hadoop05:27020")

    val settings: MongoClientSettings = MongoClientSettings.builder()
      .applyToClusterSettings(b => b.hosts(List(new ServerAddress("hadoop05:27020")).asJava)).build()
    val mongoClient: MongoClient = MongoClient(settings)

    val database: MongoDatabase = mongoClient.getDatabase("s0558059")

    val collection: MongoCollection[Document] = database.getCollection("scraped_articles")

    collection

  }

    /*val clusterSettings: ClusterSettings = ClusterSettings.builder().hosts(List(new ServerAddress("hadoop05:27020")).asJava).build()

    val settings: MongoClientSettings=MongoClientSettings.builder()
      .applyToClusterSettings((b: ClusterSettings.Builder) => b.applySettings(clusterSettings))
      .applyToSslSettings((s: SslSettings.Builder) => s.applySettings(SslSettings.builder().enabled(true).build())
      .streamFactoryFactory(NettyStreamFactoryFactory).build()

    val mongoClient: MongoClient=MongoClient(settings)*/
    /*val mongoLogger: Logger = Logger.getLogger("com.mongodb")
    mongoLogger.setLevel(Level.SEVERE)
    val clusterSettings: ClusterSettings = ClusterSettings.builder().hosts(List(new ServerAddress("hadoop05:27020")).asJava).build()
    val user: String = "s0558059"
    val databasename: String = "s0558059"
    val password: Array[Char] = "f0r313g".toCharArray
    val credential: MongoCredential = createCredential(user, databasename, password)
    val sslSetting : SslSettings = SslSettings.builder().enabled(true)

    val clientSettings: MongoClientSettings = MongoClientSettings
      .builder()
      .applyToClusterSettings((b: ClusterSettings.Builder) => b.applySettings(clusterSettings))
      .credential(credential).applyToSslSettings((b: SslSettings.Builder => ))sslSettings(SslSettings.builder().enabled(true))
      .build()


    val settings: MongoClientSettings = MongoClientSettings.builder
      .clusterSettings(clusterSettings).credentialList(List(credential,credential).asJava).sslSettings(SslSettings.builder().enabled(true).build)
      .streamFactoryFactory(NettyStreamFactoryFactory()).build()
    val mongoClient: MongoClient = MongoClient(settings)
    val database: MongoDatabase = mongoClient.getDatabase("scalatest")
    mongoClient.close()

  }
  val client:MongoClient=MongoClient("hadoop05:27020")*/


}
