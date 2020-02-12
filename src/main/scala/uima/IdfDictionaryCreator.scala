package uima

import java.io._
import java.nio.charset.StandardCharsets

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.`type`.Lemma
import org.apache.commons.io.FileUtils
import org.apache.uima.fit.component.JCasAnnotator_ImplBase
import org.apache.uima.fit.util.JCasUtil
import org.apache.uima.jcas.JCas
import spray.json._
import DefaultJsonProtocol._
import com.typesafe.config.ConfigFactory
import de.tudarmstadt.ukp.dkpro.core.api.ner.`type`.NamedEntity
import json.JSONParser
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD

/**
 * The uima component that creates the up-to-date idf model of the model created in the previous run and the documents
 * analyzed in the current run.
 */
class IdfDictionaryCreator extends JCasAnnotator_ImplBase {

  /**
   * The location to write the idf model
   */
  val modelPath: String = ConfigFactory.load().getString("app.idfmodellocationwrite")

  val sc: SparkContext = App.getSparkContext

  val oldModelLines: RDD[String] = RDDFromFile(modelPath)
  val jsonString: String = oldModelLines.fold("")(_+_)

  /**
   * The idf model created by analyzing the previous documents
   */
  val oldModel: List[(String, Double)] = {
    if(!jsonString.equals("")){JSONParser.parseIdfModel(jsonString)}
    else{List.empty[(String, Double)]}
  }

  val oldModelRdd: RDD[(String, Double)] = sc.parallelize(oldModel)

  /**
   * The number of documents that have been analyzed previously
   */
  val docCountOld: Double = {
    val dcrdd = oldModelRdd.filter(entry => entry._1.equals("$docCount$"))
    if(!dcrdd.isEmpty()){
      dcrdd.first()._2
    } else {
      0.0
    }
  }

  var termDfMap: collection.Map[String, Long] = IdfDictionaryCreator.calculateOldDf(oldModelRdd, docCountOld.toLong).collectAsMap()

  var docCountNew = 0



  /**
   * 1. step in calculating TF-IDF:
   * calculates the document frequency (df) ~ number of docs containing the term (here: Lemma or NamedEntity)
   * and saving it in termDfMap (lemma -> nrDocsContainingLemma)
   * @param aJCas
   */
  override def process(aJCas: JCas): Unit = {

    // the number of analyzed documents is increased by 1
    docCountNew+=1

    // getting all Lemma annotations in this document
    val lemmas: List[Lemma] = JCasUtil.select(aJCas, classOf[Lemma])
      .toArray
      .toList
      .asInstanceOf[List[Lemma]]

    // getting view containing named entities
    val neView: JCas = aJCas.getView("NAMED_ENTITIES_VIEW")

    // getting all NamedEntity annotations in this document
    val namedEntities: List[NamedEntity] = JCasUtil.select(neView, classOf[NamedEntity])
      .toArray
      .toList.asInstanceOf[List[NamedEntity]]

    // getting text of the current document
    val docText: String = aJCas.getDocumentText

    /* replacing lemmas which describe persons with the corresponding named entity, e.g. lemmas "Elon" and "Musk" are
    being removed from list and will be replaced by "Elon Musk" the value of the named corresponding named entity */
    val lemmasWithNamedEntities: List[Lemma] = lemmas.foldLeft(List.empty[Lemma])((list, lemma) => {
        val neWithEqualIndex = namedEntities.filter(
          ne => ne.getBegin == lemma.getBegin || ne.getEnd == lemma.getEnd)
        if(!neWithEqualIndex.isEmpty && lemma.getBegin == neWithEqualIndex.head.getBegin){
          val newLem = new Lemma(aJCas, neWithEqualIndex.head.getBegin, neWithEqualIndex.head.getEnd)
          newLem.setValue(docText.substring(neWithEqualIndex.head.getBegin, neWithEqualIndex.head.getEnd).toLowerCase)
          newLem::list
        } else if(!neWithEqualIndex.isEmpty && lemma.getEnd == neWithEqualIndex.head.getEnd) {
          list
        }
        else {
          lemma::list
        }
    })

    /* removing duplicates in lemmas and named entities, adding them to termDfMap (if the map does not contain them
    already) and increasing the df for each lemma or named entity by one*/
    termDfMap = lemmasWithNamedEntities.map(lemma => lemma.getValue)
      .toSet
      .foldLeft(termDfMap)((map, lemma) => map.updated(lemma, map.getOrElse(lemma, 0L)+1L))
  }

  /**
   * Reads the content of text file into an RDD if the file exists, else returns an empty RDD
   * @param path
   * @return RDD[String]
   */
  def RDDFromFile(path: String): RDD[String] = {
    if(!new File(path).exists()){
      println("idf-model file doesnt exist")
      return sc.emptyRDD[String]
    }
    sc.textFile(path)
  }

  /**
   * Writes the final json String into specified file.
   * @param json
   * @param fileName
   * @throws IOException
   */
  @throws[IOException]
  def serialize(json: String, fileName: String): Unit = {
    val file = new File(fileName)
    if (!file.exists) FileUtils.touch(file)
    if (file.isDirectory) throw new IOException("A directory with that name exists!")
    try {
      val out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(file, false), StandardCharsets.UTF_8))
      //val objOut = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(file)))
      try {
        out.print(json)
        out.flush()
        out.close()
      } finally if (out != null) out.close()
    }
  }

  /**
   * 2. step: calculates the Inverse Document Frequency (IDF) and saves it to a file
   * = docCountBoth(nrOfDocs previously analyzed + nrOfDocs analyzed in this run) / DF-value (nrOfDocs containing lemma)
   */
  override def collectionProcessComplete(): Unit = {
    val docCountBoth: Double = docCountOld+docCountNew
    val termDfMapRdd: RDD[(String, Long)] = sc.parallelize(termDfMap.toSeq)
    val termIdfMap: collection.Map[String, Double] = termDfMapRdd.mapValues(df => scala.math.log(docCountBoth/df.toDouble))
      .collectAsMap() + ("$docCount$" -> docCountBoth)

    val json: String = termIdfMap.toMap.toJson.compactPrint
    serialize(json, modelPath)
  }
}

object IdfDictionaryCreator extends Serializable{
  def round(x: Double): Long = Math.round(x)
  def exp(x: Double): Double = Math.exp(x)

  /**
   * Takes the idf model created in the previous run and recalculated the df values of lemmas and named entities.
   * @param oldModel
   * @param docCountOld
   * @return RDD[String, Long]
   */
  def calculateOldDf(oldModel: RDD[(String, Double)], docCountOld: Long) : RDD[(String, Long)] = {
    oldModel.filter(entry => !entry._1.equals("$docCount$"))
      .map(entry => (entry._1,
        docCountOld/round(exp(entry._2))))
  }
}