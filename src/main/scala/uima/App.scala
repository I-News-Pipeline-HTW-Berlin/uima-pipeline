package uima

import com.typesafe.config.ConfigFactory
import org.apache.spark.{SparkConf, SparkContext}
import org.apache.uima.fit.pipeline.JCasIterator

object App {

  val conf: SparkConf = new SparkConf().setMaster(ConfigFactory.load().getString("app.sparkmaster"))
  conf.set("spark.app.name", "App")
  conf.set("spark.dynamicAllocation.enabled", "true")
  conf.set("spark.executer.memory", "5g")
  conf.set("spark.driver.memory", "2g")

  val sc: SparkContext = new SparkContext(conf)
  sc.setLogLevel("ERROR")

  println(sc.getConf.toDebugString)

  def getSparkContext: SparkContext = sc

  def main(args: Array[String]) {

    val corpus: Corpus = Corpus.fromDb()

    // Execution of first pipeline (calculates IDF-model):
    val modelIt: JCasIterator = corpus.writeModel()
    modelIt.forEachRemaining(jcas => {})

    // Execution of second pipeline (calculates reading time, tfidf, named entities, most relevant lemmas, lemmas and writes outcome to db)
    val mainPipeIt: JCasIterator = corpus.mainPipeline()
    mainPipeIt.forEachRemaining(jcas => {})
  }
}
