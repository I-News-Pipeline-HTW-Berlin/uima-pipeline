import java.net.URL

import org.apache.uima.collection.CollectionReaderDescription
import de.tudarmstadt.ukp.dkpro.core.api.io.ResourceCollectionReaderBase
import de.tudarmstadt.ukp.dkpro.core.clearnlp.{ClearNlpLemmatizer, ClearNlpPosTagger, ClearNlpSegmenter}
import de.tudarmstadt.ukp.dkpro.core.io.text.TextReader
import de.tudarmstadt.ukp.dkpro.core.stopwordremover.StopWordRemover
import org.apache.uima.fit.factory.CollectionReaderFactory.createReaderDescription
import org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription
import org.apache.uima.fit.pipeline.JCasIterator
import org.apache.uima.fit.pipeline.SimplePipeline.iteratePipeline

import scala.io.Source


case class Corpus(reader: CollectionReaderDescription) {

  val STOPWORD_FILE = "src/main/resources/stopwords-de.txt"

  def tokenize(): JCasIterator =
    iteratePipeline(
      reader,
      createEngineDescription(classOf[ClearNlpSegmenter])
    ).iterator()

  def lemmatize(): JCasIterator =
    iteratePipeline(
      reader,
      createEngineDescription(classOf[ClearNlpSegmenter]),
      createEngineDescription(classOf[StopWordRemover],
        StopWordRemover.PARAM_MODEL_LOCATION, STOPWORD_FILE),
      createEngineDescription(classOf[ClearNlpPosTagger]),
      createEngineDescription(classOf[ClearNlpLemmatizer])
    ).iterator()
}

object Corpus {
  def fromDir(directory: String, pattern: String = "[+]**/*.txt", lang: String = "en"): Corpus = {
    /*Corpus(createReaderDescription(
      classOf[TextReader],
      ResourceCollectionReaderBase.PARAM_SOURCE_LOCATION, directory,
      ResourceCollectionReaderBase.PARAM_PATTERNS, pattern,
      ResourceCollectionReaderBase.PARAM_LANGUAGE, lang
    ))*/

    Corpus(createReaderDescription(
      classOf[JSONReader],
      ResourceCollectionReaderBase.PARAM_SOURCE_LOCATION, directory,
      ResourceCollectionReaderBase.PARAM_PATTERNS, pattern,
      ResourceCollectionReaderBase.PARAM_LANGUAGE, lang
    ))
  }
}