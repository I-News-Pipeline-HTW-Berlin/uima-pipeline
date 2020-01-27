package uima

import de.tudarmstadt.ukp.dkpro.core.api.ner.`type`.NamedEntity
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.`type`.Lemma
import org.apache.uima.fit.component.JCasAnnotator_ImplBase
import org.apache.uima.fit.descriptor.TypeCapability
import org.apache.uima.fit.util.JCasUtil
import org.apache.uima.jcas.JCas

@TypeCapability(
  inputs = Array("de.tudarmstadt.ukp.dkpro.core.ner.type.NamedEntity"),
  outputs = Array("de.tudarmstadt.ukp.dkpro.core.ner.type.NamedEntity")
)
class NamedEntityMapper extends JCasAnnotator_ImplBase{

  //val sc = App.getSparkContext

  override def process(aJCas: JCas): Unit = {
    //hier werden alle nicht personen rausgefiltert
    val namedEntities = JCasUtil.select(aJCas, classOf[NamedEntity]).toArray.toList.asInstanceOf[List[NamedEntity]]

    namedEntities.foreach(_.removeFromIndexes(aJCas))

    /*val namedEntitiesAfter = JCasUtil.select(aJCas, classOf[NamedEntity]).toArray.toList.asInstanceOf[List[NamedEntity]]

    println("list after:")
    println(namedEntitiesAfter)
    println()*/
    //aJCas.removeAllIncludingSubtypes(NamedEntity.`type`)
    /*for (ne <- namedEntities){
      ne.removeFromIndexes(aJCas)
    }*/

    /*val namedEntities = namedEntitiesColl
                          .toArray
                          .toList.asInstanceOf[List[NamedEntity]]*/




    val namedEntitiesOnlyPersons = namedEntities.filter(ne => ne.getValue.equalsIgnoreCase("person"))//.map(ne => (ne.getBegin, ne.getEnd))

    // .filter(ne => ne.getValue.equalsIgnoreCase("person"))
    //val namedEntitiesRdd = sc.parallelize(nesAsTuple)
    aJCas.createView("NAMED_ENTITIES_VIEW")
    val neView = aJCas.getView("NAMED_ENTITIES_VIEW")
    val namedEntitiesWithFullNames = namedEntitiesOnlyPersons.foldLeft(List.empty[NamedEntity])((list, ne) => {
      if ((!list.isEmpty) && ((list.head.getEnd + 1) == ne.getBegin)) {
        //(list.head.getBegin, lemma.getEnd)::list.tail
        val newNe = new NamedEntity(neView, list.head.getBegin, ne.getEnd)
        newNe.setValue(ne.getValue)
        newNe :: list.tail
      }
      else {
        //(ne.getBegin,ne.getEnd)::list
        val newNe = new NamedEntity(neView, ne.getBegin, ne.getEnd)
        newNe.setValue(ne.getValue)
        newNe :: list
      }
    })
    namedEntitiesWithFullNames.foreach(_.addToIndexes(neView))





    // werden wir brauchen, aber hier vielleicht nicht?
    /*val docText = aJCas.getDocumentText
    val namedEntitiesWithValues = namedEntitiesWithFullNames.map(ne => (docText.substring(ne.getBegin, ne.getEnd), ne.getValue))*/
  }
}
