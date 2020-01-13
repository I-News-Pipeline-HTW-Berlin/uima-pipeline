package uima

import java.io._

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.`type`.Lemma
import org.apache.commons.io.FileUtils
import org.apache.uima.fit.component.JCasAnnotator_ImplBase
import org.apache.uima.fit.util.{JCasUtil, LifeCycleUtil}
import org.apache.uima.jcas.JCas
import spray.json._
import DefaultJsonProtocol._
import de.tudarmstadt.ukp.dkpro.core.api.ner.`type`.NamedEntity
import json.JSONParser
import org.apache.uima.fit.descriptor.ConfigurationParameter
import org.apache.uima.fit.factory.AnalysisEngineFactory


class IdfDictionaryCreator extends JCasAnnotator_ImplBase {

  //TODO falls noch zeit sollten externe Resourcen injiziert werden (s. @ExternalResource)

  @ConfigurationParameter(name = IdfDictionaryCreator.MODEL_PATH)
  val modelPath = "src/main/resources/idfmodel.json"

  val oldModel = deserialize(modelPath)
  val docCountOld = oldModel.getOrElse("$docCount$", 0.0)

  //val jsonString: String = deserialize[String](modelPath)
  var termDfMap = oldModel.filterNot(entry => entry._1.equals("$docCount$"))
                          .map(entry => (entry._1, (docCountOld/Math.round(Math.exp(entry._2))).toLong))


 // var termDfMap = Map.empty[String, Int]

  var docCountNew = 0

  /*override def initialize(context: UimaContext): Unit = {
    super.initialize(context)
    //val dfStore = new DfStore(())

  }*/

  /**
   * 1. step in calculating TF-IDF:
   * calculates the document frequency (df) ~ number of docs containing the term (here: lemma)
   * and saving it in termDfMap (lemma -> nrDocsContainingLemma)
   * @param aJCas
   */
  override def process(aJCas: JCas): Unit = {
    docCountNew+=1
    val lemmas = JCasUtil.select(aJCas, classOf[Lemma]).toArray().toList.asInstanceOf[List[Lemma]]
    val namedEntitiesView = aJCas.getView("NAMED_ENTITIES_VIEW")
    val namedEntities = JCasUtil.select(namedEntitiesView, classOf[NamedEntity]).toArray.toList.asInstanceOf[List[NamedEntity]]
    val docText = aJCas.getDocumentText

    //hier werden lemmas wie "Elon" und "Musk" ersetzt durch "Elon Musk"
    // erstmal nur für personen, TODO: überlegen, was und ob wir mit den restlichen namedEntities machen
    val lemmasWithNamedEntities = lemmas.foldLeft(List.empty[Lemma])((list, lemma) => {
        val neWithEqualIndex = namedEntities.filter(
          ne => ne.getValue.equalsIgnoreCase("person") && (ne.getBegin == lemma.getBegin || ne.getEnd == lemma.getEnd))
        if(!neWithEqualIndex.isEmpty && lemma.getBegin == neWithEqualIndex.head.getBegin){
          val newLem = new Lemma(aJCas, neWithEqualIndex.head.getBegin, neWithEqualIndex.head.getEnd)
          newLem.setValue(docText.substring(neWithEqualIndex.head.getBegin, neWithEqualIndex.head.getEnd))
          newLem::list
        } else if(!neWithEqualIndex.isEmpty && lemma.getEnd == neWithEqualIndex.head.getEnd) {
          list
        }
        else {
          lemma::list
        }
    })

    //df
    termDfMap = lemmasWithNamedEntities.map(lemma => lemma.asInstanceOf[Lemma].getValue)
      .toSet
      .foldLeft(termDfMap)((map, lemma) => map.updated(lemma, map.getOrElse(lemma, 0L)+1L))
   /* println("content of size view: "+aJCas.getView("SIZE_VIEW").getDocumentText)
    if(docCount >= aJCas.getView("SIZE_VIEW").getDocumentText.toInt){
      println("now please end process")
      val desc = AnalysisEngineFactory.createEngineDescription(this.getClass)
      LifeCycleUtil.collectionProcessComplete(AnalysisEngineFactory.createEngine(desc))
    }*/
  }

  @SuppressWarnings(Array("unchecked"))
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
  }

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

  /**
   * 2. step: calculates the Inverse Document Frequency (IDF) and saves it to a file
   * = docCountBoth(nrOfDocs in total at the current moment) / DF-value (nrOfDocs containing lemma)
   */
  override def collectionProcessComplete(): Unit = {
    val docCountBoth = docCountOld+docCountNew
    val termIdfMap = termDfMap.view.mapValues(df => scala.math.log(docCountBoth/df.toDouble)).toMap + ("$docCount$" -> docCountBoth)
    val json = termIdfMap.toJson.compactPrint
   /* println("Size of map: "+termIdfMap.size)
    println(termIdfMap)
    println(json)*/
    serialize(json, modelPath)
  }
}

object IdfDictionaryCreator {
  final val MODEL_PATH = "src/main/resources/idfmodel.json"
}