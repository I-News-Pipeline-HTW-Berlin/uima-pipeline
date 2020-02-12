package uima

import de.tudarmstadt.ukp.dkpro.core.api.ner.`type`.NamedEntity
import org.apache.uima.fit.component.JCasAnnotator_ImplBase
import org.apache.uima.fit.descriptor.TypeCapability
import org.apache.uima.fit.util.JCasUtil
import org.apache.uima.jcas.JCas

/**
 * This component takes the NamedEntity annotations from the pipeline, removes all NamedEntities that are not persons
 * and combines first and last name of persons.
 */
@TypeCapability(
  inputs = Array("de.tudarmstadt.ukp.dkpro.core.ner.type.NamedEntity"),
  outputs = Array("de.tudarmstadt.ukp.dkpro.core.ner.type.NamedEntity")
)
class NamedEntityMapper extends JCasAnnotator_ImplBase{

  /**
   * Removes previously found NamedEntity annotations from indexes, creates new NamedEntity annotations by joining first
   * and last name of persons by comparing indexes and adds the new NamedEntity annotations to indexes.
   * @param aJCas
   */
  override def process(aJCas: JCas): Unit = {

    // get all named entities
    val namedEntities: List[NamedEntity] = JCasUtil.select(aJCas, classOf[NamedEntity]).toArray.toList.asInstanceOf[List[NamedEntity]]

    // remove all named entities from indexes
    namedEntities.foreach(_.removeFromIndexes(aJCas))

    // remove named entities that are not persons from list
    val namedEntitiesOnlyPersons: List[NamedEntity] = namedEntities.filter(ne => ne.getValue.equalsIgnoreCase("person"))

    // create view for new named entities
    aJCas.createView("NAMED_ENTITIES_VIEW")
    val neView = aJCas.getView("NAMED_ENTITIES_VIEW")

    // join first and last name
    val namedEntitiesWithFullNames: List[NamedEntity] = namedEntitiesOnlyPersons.foldLeft(List.empty[NamedEntity])((list, ne) => {
      if ((!list.isEmpty) && ((list.head.getEnd + 1) == ne.getBegin)) {
        val newNe = new NamedEntity(neView, list.head.getBegin, ne.getEnd)
        newNe.setValue(ne.getValue)
        newNe :: list.tail
      }
      else {
        val newNe = new NamedEntity(neView, ne.getBegin, ne.getEnd)
        newNe.setValue(ne.getValue)
        newNe :: list
      }
    })

    // add new named entities to indexes
    namedEntitiesWithFullNames.foreach(_.addToIndexes(neView))
  }
}
