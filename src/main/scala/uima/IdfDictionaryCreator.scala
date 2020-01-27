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
import org.apache.spark.rdd.RDD
import org.apache.uima.fit.descriptor.ConfigurationParameter


class IdfDictionaryCreator extends JCasAnnotator_ImplBase {

  //TODO falls noch zeit sollten externe Resourcen injiziert werden (s. @ExternalResource)

  //@ConfigurationParameter(name = IdfDictionaryCreator.MODEL_PATH)
  val modelPath: String = ConfigFactory.load().getString("app.idfmodellocationwrite")

  val sc = App.getSparkContext

  val oldModelLines = RDDFromFile(modelPath)
  val jsonString = oldModelLines.fold("")(_+_)
  val oldModel = {
    if(!jsonString.equals("")){JSONParser.parseIdfModel(jsonString)}
    else{List.empty[(String, Double)]}
  }
  //val docCountOld = oldModel.getOrElse("$docCount$", 0.0)
  val oldModelRdd = sc.parallelize(oldModel)
  val docCountOld = {
    val dcrdd = oldModelRdd.filter(entry => entry._1.equals("$docCount$"))
    if(!dcrdd.isEmpty()){
      dcrdd.first()._2
    } else {
      0.0
    }
  }

  //val oldModel = deserialize(modelPath)


 // var termDfMap = oldModel.filterNot(entry => entry._1.equals("$docCount$"))
  //                        .map(entry => (entry._1, (docCountOld/Math.round(Math.exp(entry._2))).toLong))
  //val roundFunc = IdfDictionaryCreator.round _
  //val expFunc = IdfDictionaryCreator.exp _
  /*var termDfMap = oldModelRdd.filter(entry => !entry._1.equals("$docCount$"))
                          .map(entry => (entry._1,
                            (docCountOld/IdfDictionaryCreator.round(IdfDictionaryCreator.exp(entry._2))).toLong))
                          .collectAsMap()*/
  var termDfMap = IdfDictionaryCreator.calculateOldDf(oldModelRdd, docCountOld.toLong).collectAsMap()

  var docCountNew = 0



  /**
   * 1. step in calculating TF-IDF:
   * calculates the document frequency (df) ~ number of docs containing the term (here: lemma)
   * and saving it in termDfMap (lemma -> nrDocsContainingLemma)
   * @param aJCas
   */
  override def process(aJCas: JCas): Unit = {
    docCountNew+=1
    val lemmas = JCasUtil.select(aJCas, classOf[Lemma])
      .toArray
      .toList
      .asInstanceOf[List[Lemma]]
      //.map(l => (l.getBegin, l.getEnd, l.getValue))

   // val lemmasRdd = sc.parallelize(lemmas)

    val neView = aJCas.getView("NAMED_ENTITIES_VIEW")

    val namedEntities = JCasUtil.select(neView, classOf[NamedEntity])
      .toArray
      .toList.asInstanceOf[List[NamedEntity]]
      //.map(ne => (ne.getBegin, ne.getEnd))

    //val namedEntitiesRdd = sc.parallelize(namedEntities)

    val docText = aJCas.getDocumentText

    //hier werden lemmas wie "Elon" und "Musk" ersetzt durch "Elon Musk", dh. Duplikate von lemmas und namedEntities werden entfernt
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
    val lemmasWithNamedEntities = lemmas.foldLeft(List.empty[Lemma])((list, lemma) => {
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
    //val lemmasWithNamedEntitiesRdd = sc.parallelize(lemmasWithNamedEntities)
    //df
    termDfMap = lemmasWithNamedEntities.map(lemma => lemma.getValue)
      .toSet
      .foldLeft(termDfMap)((map, lemma) => map.updated(lemma, map.getOrElse(lemma, 0L)+1L))
  }

  /*private def getPath(path: String, isAResource: Boolean): String = isAResource match{
    case true => getClass.getClassLoader.getResource(path).getPath
    case false => path
  }*/

  def RDDFromFile(path: String, isAResource: Boolean = true): RDD[String] = {
    //val acref = getPath(path, isAResource)
    if(!new File(path).exists()){
      println("idf-model file doesnt exist")
      return sc.emptyRDD[String]
    }
    sc.textFile(path)
  }

 /* @SuppressWarnings(Array("unchecked"))
  @throws[IOException]
  def deserialize(filePath: String): Map[String, Double] = try {
    if(! new File(filePath).exists()) {
      println("idf-model file doesnt exist")
      return Map.empty[String, Double]
    }
    val in = new ObjectInputStream(new FileInputStream(new File(filePath)))
    try{
      val jsonString = in.readObject.asInstanceOf[String]
      JSONParser.parseIdfModel(jsonString)
    }
    catch {
          // warum classnotfoundexception?
      case e: ClassNotFoundException =>
        throw new IOException(e)
    } finally if (in != null) in.close()
  }*/

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
   * = docCountBoth(nrOfDocs in total at the current moment) / DF-value (nrOfDocs containing lemma)
   */
  override def collectionProcessComplete(): Unit = {
    val docCountBoth = docCountOld+docCountNew
    val termDfMapRdd = sc.parallelize(termDfMap.toSeq)
    val termIdfMap = termDfMapRdd.mapValues(df => scala.math.log(docCountBoth/df.toDouble))
      .collectAsMap() + ("$docCount$" -> docCountBoth)


    val json = termIdfMap.toMap.toJson.compactPrint
    serialize(json, modelPath)
  }
}

object IdfDictionaryCreator extends Serializable{
  //final val MODEL_PATH = "modelPath"
  def round(x: Double) = Math.round(x)
  def exp(x: Double) = Math.exp(x)
  def calculateOldDf(oldModel: RDD[(String, Double)], docCountOld: Long) : RDD[(String, Long)] = {
    oldModel.filter(entry => !entry._1.equals("$docCount$"))
      .map(entry => (entry._1,
        docCountOld/round(exp(entry._2))))
  }
}