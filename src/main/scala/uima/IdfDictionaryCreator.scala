package uima

import java.io._

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.`type`.Lemma
import org.apache.commons.io.FileUtils
import org.apache.uima.fit.component.JCasAnnotator_ImplBase
import org.apache.uima.fit.util.JCasUtil
import org.apache.uima.jcas.JCas
import spray.json.{JsFalse, JsNumber, JsString, JsTrue, JsValue, JsonFormat}


class IdfDictionaryCreator extends JCasAnnotator_ImplBase {

  val termIdfMap = Map.empty[String, Int]

  var docCount = 0
  /*override def initialize(context: UimaContext): Unit = {
    super.initialize(context)
    //val dfStore = new DfStore(())

  }*/

  implicit object AnyJsonFormat extends JsonFormat[Any] {
    def write(x: Any) = x match {
      case n: Int => JsNumber(n)
      case s: String => JsString(s)
      case b: Boolean if b => JsTrue
      case b: Boolean if !b => JsFalse
    }
    def read(value: JsValue) = value match {
      case JsNumber(n) => n.intValue
      case JsString(s) => s
      case JsTrue => true
      case JsFalse => false
    }
  }

  override def process(aJCas: JCas): Unit = {

    docCount+=1
    val lemmas = JCasUtil.select(aJCas, classOf[Lemma]).toArray.toList

    //df
    lemmas.map(lemma => lemma.asInstanceOf[Lemma].getValue).toSet
      .map(lemma => termIdfMap.updated(lemma, termIdfMap.getOrElse(lemma, 0)+1))   // Set[Map[String, Int]]

   /* Error:(46, 12) missing parameter type
    .map(lemma => lemma.asInstanceOf[Lemma].getValue) */


    //lemmas.map(lemma => lemma.asInstanceOf[Lemma].getValue).filter(lemma => !termMap.contains(lemma))
  }

  @throws[IOException]
  def serialize(obj: Any, fileName: String): Unit = {
    val file = new File(fileName)
    if (!file.exists) FileUtils.touch(file)
    if (file.isDirectory) throw new IOException("A directory with that name exists!")
    try {
      val objOut = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(file)))
      try {
        objOut.writeObject(obj)
        objOut.flush()
        objOut.close()
      } finally if (objOut != null) objOut.close()
    }
  }



  override def collectionProcessComplete(): Unit = {

    termIdfMap.view.mapValues(df => docCount/df)
    val a = AnyJsonFormat.write(termIdfMap).compactPrint
    println(a)


  }



}
