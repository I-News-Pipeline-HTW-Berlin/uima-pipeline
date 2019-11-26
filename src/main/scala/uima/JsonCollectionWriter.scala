package uima

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.`type`.Lemma
import org.apache.uima.fit.component.JCasConsumer_ImplBase
import org.apache.uima.fit.util.JCasUtil
import org.apache.uima.jcas.JCas
import spray.json._
import DefaultJsonProtocol._
import de.tudarmstadt.ukp.dkpro.core.api.metadata.`type`.MetaDataStringField
import json.JSONComposer

class JsonCollectionWriter extends JCasConsumer_ImplBase {
  override def process(aJCas: JCas): Unit = {

    val lemmas = JCasUtil.select(aJCas, classOf[Lemma]).toArray.toList.asInstanceOf[List[Lemma]].map(lem => lem.getValue)
    val readingTime = JCasUtil.select(aJCas, classOf[MetaDataStringField]).toArray.toList.head.asInstanceOf[MetaDataStringField].getValue.toInt

    val jsonString = JSONComposer.compose(lemmas, readingTime)
    println(jsonString)
  }
}
