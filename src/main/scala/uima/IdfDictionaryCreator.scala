package uima

import java.io._

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.`type`.Lemma
import org.apache.commons.io.FileUtils
import org.apache.uima.fit.component.JCasAnnotator_ImplBase
import org.apache.uima.fit.util.{JCasUtil, LifeCycleUtil}
import org.apache.uima.jcas.JCas
import spray.json._
import DefaultJsonProtocol._
import json.JSONParser
import org.apache.uima.fit.descriptor.ConfigurationParameter
import org.apache.uima.fit.factory.AnalysisEngineFactory


class IdfDictionaryCreator extends JCasAnnotator_ImplBase {

  @ConfigurationParameter(name = IdfDictionaryCreator.MODEL_PATH)
  val modelPath = "src/main/resources/tfidfmodel.json"
  val oldModel = deserialize(modelPath)
  val docCountOld = oldModel.getOrElse("$docCount$", 0)

  //val jsonString: String = deserialize[String](modelPath)
  var termDfMap = oldModel.filterNot(entry => entry._1.equals("$docCount$"))
                          .map(entry => (entry._1, docCountOld/entry._2))

 // var termDfMap = Map.empty[String, Int]

  var docCountNew = 0

  /*override def initialize(context: UimaContext): Unit = {
    super.initialize(context)
    //val dfStore = new DfStore(())

  }*/


  override def process(aJCas: JCas): Unit = {
    docCountNew+=1
    val lemmas = JCasUtil.select(aJCas, classOf[Lemma]).toArray().toList
    //df
    termDfMap = lemmas.map(lemma => lemma.asInstanceOf[Lemma].getValue)
      .toSet
      .foldLeft(termDfMap)((map, lemma) => map.updated(lemma, map.getOrElse(lemma, 0)+1))
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
    val file = new File(filePath)
    if(!file.exists) Map.empty[String, Double]
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

  //Problem: Diese Methode wird erst am Ende der Pipeline aufgerufen, was für uns zu spät ist
  override def collectionProcessComplete(): Unit = {
    val docCountBoth = docCountOld+docCountNew
    val termIdfMap = termDfMap.view.mapValues(df => docCountBoth/df.toDouble).toMap + ("$docCount$" -> docCountBoth)
    val json = termIdfMap.toJson.compactPrint
   /* println("Size of map: "+termIdfMap.size)
    println(termIdfMap)
    println(json)*/
    serialize(json, modelPath)
  }
}

object IdfDictionaryCreator {
  final val MODEL_PATH = "src/main/resources/tfidfmodel.json"
}