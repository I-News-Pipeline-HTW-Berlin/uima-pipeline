package uima

import java.io.{File, FileInputStream, IOException, ObjectInputStream}

import com.typesafe.config.ConfigFactory
import de.tudarmstadt.ukp.dkpro.core.api.ner.`type`.NamedEntity
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.`type`.Lemma
import org.apache.spark.rdd.RDD
import org.apache.uima.fit.component.JCasAnnotator_ImplBase
import org.apache.uima.fit.descriptor.ConfigurationParameter
import org.apache.uima.fit.util.JCasUtil
import org.apache.uima.jcas.JCas
import spray.json.DefaultJsonProtocol._
import spray.json._

class TfIdfCalculator extends JCasAnnotator_ImplBase {

  //@ConfigurationParameter(name = TfIdfCalculator.MODEL_PATH)
  val modelPath: String = ConfigFactory.load().getString("app.idfmodellocationread")

  //@ConfigurationParameter(name = TfIdfCalculator.PERCENT_OF_LEMMAS)
  val percentOfLemmas: String = ConfigFactory.load().getString("app.percentoflemmas")

  val sc = App.getSparkContext
  val modelLines = RDDFromFile(modelPath)
  val jsonString = modelLines.fold("")(_+_)

  // deserialize tfidfmodel.json and read in as map
  //val jsonString: String = deserialize[String](modelPath)
  val jsonAst: JsValue = jsonString.parseJson
  val termIdfMap = jsonAst.convertTo[Map[String, Double]]

  // calculate TF(w) = (Number of times term w appears in a document) / (Total number of terms in the document)
  override def process(aJCas: JCas): Unit = {

    val lemmas = JCasUtil.select(aJCas, classOf[Lemma])
                    .toArray()
                    .toList.asInstanceOf[List[Lemma]]
                    .map(l => (l.getBegin, l.getEnd, l.getValue))

    //val lemmasRdd = sc.parallelize(lemmas)
    val namedEntitiesView = aJCas.getView("NAMED_ENTITIES_VIEW")
    val namedEntities = JCasUtil.select(namedEntitiesView, classOf[NamedEntity])
                    .toArray
                    .toList.asInstanceOf[List[NamedEntity]]
                    //.map(ne => (ne.getBegin, ne.getEnd))

    val docText = aJCas.getDocumentText

    //hier werden lemmas wie "Elon" und "Musk" ersetzt durch "Elon Musk"
    // erstmal nur für personen, TODO: überlegen, was und ob wir mit den restlichen namedEntities machen
    val lemmasWithNamedEntities = lemmas.foldLeft(List.empty[(Int, Int, String)])((list, lemma) => {
      val neWithEqualIndex = namedEntities.filter(
        ne => ne.getBegin == lemma._1 || ne.getEnd == lemma._2)
      if(!neWithEqualIndex.isEmpty && lemma._1 == neWithEqualIndex.head.getBegin){
        /*val newLem = new Lemma(aJCas, neWithEqualIndex.head.getBegin, neWithEqualIndex.head.getEnd)
        newLem.setValue(docText.substring(neWithEqualIndex.head.getBegin, neWithEqualIndex.head.getEnd).toLowerCase)
        newLem::list*/
        (neWithEqualIndex.head.getBegin,
          neWithEqualIndex.head.getEnd,
          docText.substring(neWithEqualIndex.head.getBegin, neWithEqualIndex.head.getEnd).toLowerCase)::list
      } else if(!neWithEqualIndex.isEmpty && lemma._2 == neWithEqualIndex.head.getEnd) {
        list
      }
      else {
        lemma::list
      }
    })

    /*val lemmasWithNamedEntities = lemmasRdd.aggregate(List.empty[(Int, Int, String)])((list, lemma) => {
      val neWithEqualIndex = namedEntities.filter(
        ne => ne._1 == lemma._1 || ne._2 == lemma._2)
      if(!neWithEqualIndex.isEmpty && lemma._1 == neWithEqualIndex.head._1){
        (neWithEqualIndex.head._1,
          neWithEqualIndex.head._2,
          docText.substring(neWithEqualIndex.head._1, neWithEqualIndex.head._2))::list
      } else if(!neWithEqualIndex.isEmpty && lemma._2 == neWithEqualIndex.head._2) {
        list
      }
      else {
        lemma::list
      }
    }, (l1, l2) => l1:::l2)*/

    val lemmasWithNamedEntitiesRdd = sc.parallelize(lemmasWithNamedEntities)

    val nrOfLemmas = lemmasWithNamedEntitiesRdd.count

    // create map with lemmas and their tf-values
    val tfMap = lemmasWithNamedEntitiesRdd
      .map(lemma => lemma._3)
      .groupBy(x => x)
      //.reduceByKey(l => l.size/ nrOfLemmas.toDouble)
      .mapValues(_.size / nrOfLemmas.toDouble)

    // create map with lemmas and their calculated tfIdf-values

    /*val tfidfMap = tfMap.map(lemma => ( {
      val anno = new Lemma(mostRelevantView)
      anno.setValue(lemma._1)
      anno
    }, lemma._2 * termIdfMap(lemma._1)))*/
    //val tfidfMap = tfMap.map(lemma => (lemma._1, lemma._2 * termIdfMap(lemma._1)))
    val tfidfMap = TfIdfCalculator.createTfidfMap(tfMap, termIdfMap)

    aJCas.createView("MOST_RELEVANT_VIEW")
    val mostRelevantView = aJCas.getView("MOST_RELEVANT_VIEW")
    // get n most relevant (lemmas with highest tfidf values)
    val mostRelevantLemmas = getMostRelevant((nrOfLemmas * percentOfLemmas.toDouble).toInt, tfidfMap)
                      .map(lemma =>  {
                        val anno = new Lemma(mostRelevantView)
                        anno.setValue(lemma)
                        anno})

    mostRelevantLemmas.foreach(_.addToIndexes())
  }

  def getMostRelevant(amount: Int, tfIdfMap: RDD[(String, Double)]): Array[String] = {
    tfIdfMap.sortBy(_._2, ascending = false).keys.distinct.take(amount)
    //List(tfIdfMap.toSeq.sortWith(_._2 > _._2): _*).take(amount).toMap.keys.toList
  }

  /*@SuppressWarnings(Array("unchecked"))
  @throws[IOException]
  def deserialize[T](filePath: String): T = try {
    val in = new ObjectInputStream(new FileInputStream(new File(filePath)))
    try in.readObject.asInstanceOf[T]
    catch {
      case e: ClassNotFoundException =>
        throw new IOException(e)
    } finally if (in != null) in.close()
  }*/

  def RDDFromFile(path: String, isAResource: Boolean = true): RDD[String] = {
    //val acref = getPath(path, isAResource)
    if(!new File(path).exists()){
      println("idf-model file doesnt exist")
      return sc.emptyRDD[String]
    }
    sc.textFile(path)
  }

}

object TfIdfCalculator extends Serializable {

  def createTfidfMap(tfMap: RDD[(String, Double)], termIdfMap: Map[String, Double] ) : RDD[(String, Double)] = {
    tfMap.map(lemma => (lemma._1, lemma._2 * termIdfMap(lemma._1)))
  }
  //final val MODEL_PATH = "modelPath"
  //final val PERCENT_OF_LEMMAS = "percentOfLemmas"
}


