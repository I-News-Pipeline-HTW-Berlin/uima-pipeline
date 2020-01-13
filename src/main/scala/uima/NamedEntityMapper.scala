package uima

import de.tudarmstadt.ukp.dkpro.core.api.ner.`type`.NamedEntity
import org.apache.uima.fit.component.JCasAnnotator_ImplBase
import org.apache.uima.fit.util.JCasUtil
import org.apache.uima.jcas.JCas

class NamedEntityMapper extends JCasAnnotator_ImplBase{

  override def process(aJCas: JCas): Unit = {
    aJCas.createView("NAMED_ENTITIES_VIEW")
    val namedEntitiesView = aJCas.getView("NAMED_ENTITIES_VIEW")
    val namedEntities = JCasUtil.select(aJCas, classOf[NamedEntity]).toArray.toList.asInstanceOf[List[NamedEntity]]//.filter(ne => !ne.getValue.equals("0"))
    val namedEntitiesWithFullNames = namedEntities.foldLeft(List.empty[NamedEntity])((l, ne) => {
      if((!l.isEmpty) && ((l.head.getEnd + 1) == ne.getBegin) && l.head.getValue.equals(ne.getValue)){
        val newNe = new NamedEntity(namedEntitiesView, l.head.getBegin, ne.getEnd)
        newNe.setValue(ne.getValue)
        newNe::l.tail
      }
      else{
        val newNe = new NamedEntity(namedEntitiesView, ne.getBegin, ne.getEnd)
        newNe.setValue(ne.getValue)
        newNe::l
      }
    })

    namedEntitiesWithFullNames.foreach(_.addToIndexes(namedEntitiesView))

    // werden wir brauchen, aber hier vielleicht nicht?
    /*val docText = aJCas.getDocumentText
    val namedEntitiesWithValues = namedEntitiesWithFullNames.map(ne => (docText.substring(ne.getBegin, ne.getEnd), ne.getValue))*/
  }
}
