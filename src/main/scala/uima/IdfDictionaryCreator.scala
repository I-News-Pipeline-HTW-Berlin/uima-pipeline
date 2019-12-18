package uima

import java.io.{BufferedOutputStream, File, FileOutputStream, IOException, ObjectOutputStream}

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.`type`.Lemma
import de.tudarmstadt.ukp.dkpro.core.frequency.tfidf.model.DfStore
import org.apache.commons.io.FileUtils
import org.apache.uima.UimaContext
import org.apache.uima.fit.component.JCasAnnotator_ImplBase
import org.apache.uima.fit.util.JCasUtil
import org.apache.uima.jcas.JCas

class IdfDictionaryCreator extends JCasAnnotator_ImplBase {

  val termIdfMap = Map.empty[String, Int]
  var docCount = 0
  /*override def initialize(context: UimaContext): Unit = {
    super.initialize(context)
    //val dfStore = new DfStore(())

  }*/

  override def process(aJCas: JCas): Unit = {
    docCount+=1
    val lemmas = JCasUtil.select(aJCas, classOf[Lemma]).toArray.toList
    //df
    lemmas.map(lemma => lemma.asInstanceOf[Lemma].getValue).toSet
      .map(lemma => termIdfMap.updated(lemma, termIdfMap.getOrElse(lemma, 0)+1))
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
    //idf
    termIdfMap.view.mapValues(df => docCount/df)
  }


}
