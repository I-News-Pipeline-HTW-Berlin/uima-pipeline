package uima

import java.io._

import com.typesafe.config.ConfigFactory
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.`type`.Lemma
import json.JSONParser
import org.apache.commons.io.FileUtils
import org.apache.uima.fit.component.JCasAnnotator_ImplBase
import org.apache.uima.fit.util.JCasUtil
import org.apache.uima.jcas.JCas
import spray.json.DefaultJsonProtocol._
import spray.json._


/**
 * The uima component that creates the up-to-date idf model of the model created in the previous run and the documents
 * analyzed in the current run.
 */
class IdfDictionaryCreator extends JCasAnnotator_ImplBase {

  /**
   * The location to write the idf model
   */
  val modelPath: String = ConfigFactory.load().getString("app.idfmodellocationwrite")

  /**
   * The idf model created by analyzing the previous documents
   */
  val oldModel: Map[String, Double] = deserialize(modelPath)

  /**
   * The number of documents that have been analyzed previously
   */
  val docCountOld: Double = oldModel.getOrElse("$docCount$", 0.0)

  /**
   * Takes the idf model created in the previous run and recalculates the df values of lemmas.
   *
   * @return Map[String, Long]
   */
  var termDfMap: Map[String, Long] = oldModel.filterNot(entry => entry._1.equals("$docCount$"))
    .map(entry => (entry._1, (docCountOld / Math.round(Math.exp(entry._2))).toLong))

  var docCountNew = 0


  /**
   * 1. step in calculating TF-IDF:
   * calculates the document frequency (df) ~ number of docs containing the term (here: Lemma)
   * and saving it in termDfMap (lemma -> nrDocsContainingLemma)
   *
   * @param aJCas
   */
  override def process(aJCas: JCas): Unit = {

    // increment the number of analyzed documents by 1
    docCountNew += 1

    // get all Lemma annotations in this document
    val lemmas = JCasUtil.select(aJCas, classOf[Lemma]).toArray().toList.asInstanceOf[List[Lemma]]

    /* remove duplicates in lemmas, add them to termDfMap (if not yet in the map)
    and increase the df for each lemma by one */
    termDfMap = lemmas.map(lemma => lemma.getValue)
      .toSet
      .foldLeft(termDfMap)((map, lemma) => map.updated(lemma, map.getOrElse(lemma, 0L) + 1L))
  }

  /**
   * 2. step: calculates the Inverse Document Frequency (IDF) and saves it to a file
   * = docCountBoth(nrOfDocs previously analyzed + nrOfDocs analyzed in this run) / DF-value (nrOfDocs containing lemma)
   */
  override def collectionProcessComplete(): Unit = {
    val docCountBoth = docCountOld + docCountNew
    val termIdfMap = termDfMap.view.mapValues(df => scala.math.log(docCountBoth / df.toDouble)).toMap + ("$docCount$" -> docCountBoth)
    val json = termIdfMap.toJson.compactPrint
    serialize(json, modelPath)
  }

  /**
   * Reads and deserializes json file from given path
   *
   * @param filePath
   * @throws IOException
   * @return Map[String, Double]
   */
  @SuppressWarnings(Array("unchecked"))
  @throws[IOException]
  def deserialize(filePath: String): Map[String, Double] = try {
    if (!new File(filePath).exists()) {
      println("idf-model file doesnt exist")
      return Map.empty[String, Double]
    }
    val in = new ObjectInputStream(new FileInputStream(new File(filePath)))
    try {
      val jsonString = in.readObject.asInstanceOf[String]
      JSONParser.parseIdfModel(jsonString)
    }
    catch {
      case e: ClassNotFoundException =>
        throw new IOException(e)
    } finally if (in != null) in.close()
  }

  /**
   * Writes the final json String into specified file.
   *
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
      val objOut = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(file)))
      try {
        objOut.writeObject(json)
        objOut.flush()
        objOut.close()
      } finally if (objOut != null) objOut.close()
    }
  }

}
