package uima


import com.typesafe.config.ConfigFactory
import de.tudarmstadt.ukp.dkpro.core.ixa.IxaLemmatizer
import de.tudarmstadt.ukp.dkpro.core.opennlp.{OpenNlpPosTagger, OpenNlpSegmenter}
import de.tudarmstadt.ukp.dkpro.core.stopwordremover.StopWordRemover
import de.tudarmstadt.ukp.dkpro.core.textnormalizer.annotations.TrailingCharacterRemover
import de.tudarmstadt.ukp.dkpro.core.tokit.TokenTrimmer
import org.apache.uima.collection.CollectionReaderDescription
import org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription
import org.apache.uima.fit.factory.CollectionReaderFactory.createReaderDescription
import org.apache.uima.fit.pipeline.JCasIterator
import org.apache.uima.fit.pipeline.SimplePipeline.iteratePipeline

case class Corpus(reader: CollectionReaderDescription, readerForModel: CollectionReaderDescription) {

  /**
   * Path to file containing stop words
   */
  val STOPWORD_FILE: String = ConfigFactory.load().getString("app.stopwordfile")

  /**
   * Model for part of speech tagging
   */
  val POS_TAGGER_DE_MODEL: String = ConfigFactory.load().getString("app.postaggermodel")

  /**
   * Model for token segmentation
   */
  val SEGMENTER_DE_TOKEN_MODEL: String = ConfigFactory.load().getString("app.segmentertokenmodel")

  /**
   * Model for sentence segmentation
   */
  val SEGMENTER_DE_SENTENCE_MODEL: String = ConfigFactory.load().getString("app.segmentersentencemodel")

  /**
   * Language of the documents to be analyzed
   */
  val LANGUAGE: String = ConfigFactory.load().getString("app.language")

  /**
   * Artifact uri of model for lemmatizer
   */
  val LEMMATIZER_DE_MODEL: String = ConfigFactory.load().getString("app.lemmatizermodelartifacturi")

  /**
   * Creates the pipeline to write model (first pipeline). The pipeline is constructed of the following components:
   * - a reader (see ReaderFirstPipeline)
   * - OpenNlpSegmenter (splits text into tokens, see DKPro documentation)
   * - TokenTrimmer (removes unwanted leading characters from token, see DKPro documentation)
   * - TrailingCharacterRemover (removes unwanted trailing characters from token, see DKPro documentation)
   * - StopWordRemover (removes Tokens that match words specified in STOPWORD_FILE, see DKPro documentation)
   * - OpenNlpPosTagger (creates a POS annotation for each Token annotation, see DKPro documentation)
   * - IxaLemmatizer (creates a Lemma annotation for each Token annotation, see DKPro documentation)
   * - IdfDictionaryCreator (creates idf dictionary, see class IdfDictionaryCreator)
   *
   * @return a JCasIterator
   */
  def writeModel(): JCasIterator =
    iteratePipeline(
      readerForModel,
      createEngineDescription(classOf[OpenNlpSegmenter],
        OpenNlpSegmenter.PARAM_TOKENIZATION_MODEL_LOCATION, SEGMENTER_DE_TOKEN_MODEL,
        OpenNlpSegmenter.PARAM_SEGMENTATION_MODEL_LOCATION, SEGMENTER_DE_SENTENCE_MODEL,
        OpenNlpSegmenter.PARAM_LANGUAGE, "de"),
      createEngineDescription(classOf[TokenTrimmer],
        TokenTrimmer.PARAM_PREFIXES, Array(":", "\"", ".", "|", "“", "„", "-", "_", "—", "–", "\u00AD", "‚", "‘", "?", "?", "…", "!", ";", "(", "(", "(", ")", ")", ")", ")"),
        TokenTrimmer.PARAM_SUFFIXES, Array()),
      createEngineDescription(classOf[TrailingCharacterRemover],
        TrailingCharacterRemover.PARAM_MIN_TOKEN_LENGTH, 1,
        TrailingCharacterRemover.PARAM_PATTERN, "[\\Q,‚‘.:|_„\u00AD-–??!;“^»*’…((()))&/\"'©§'—«·=\\E0-9]+"),
      createEngineDescription(classOf[StopWordRemover],
        StopWordRemover.PARAM_MODEL_LOCATION, STOPWORD_FILE),
      createEngineDescription(classOf[OpenNlpPosTagger],
        OpenNlpPosTagger.PARAM_MODEL_LOCATION, POS_TAGGER_DE_MODEL,
        OpenNlpPosTagger.PARAM_LANGUAGE, LANGUAGE),
      createEngineDescription(classOf[IxaLemmatizer],
        IxaLemmatizer.PARAM_MODEL_ARTIFACT_URI, LEMMATIZER_DE_MODEL,
        IxaLemmatizer.PARAM_LANGUAGE, LANGUAGE),
      createEngineDescription(classOf[IdfDictionaryCreator])
    ).iterator()

  /**
   * Creates the main (second) pipeline. The pipeline is constructed of the following components:
   * - a reader (see ReaderSecondPipeline)
   * - OpenNlpSegmenter (splits text into tokens, see DKPro documentation)
   * - TokenTrimmer (removes unwanted leading characters from token, see DKPro documentation)
   * - TrailingCharacterRemover (removes unwanted trailing characters from token, see DKPro documentation)
   * - ReadingTimeEstimator (see class ReadingTimeEstimator)
   * - StopWordRemover (removes Tokens that match words specified in STOPWORD_FILE, see DKPro documentation)
   * - OpenNlpPosTagger (creates a POS annotation for each Token annotation, see DKPro documentation)
   * - IxaLemmatizer (creates a Lemma annotation for each Token annotation, see DKPro documentation)
   * - TfIdfCalculator (see class TfIdfCalculator)
   * - JsonWriter (see class JsonWriter).
   *
   * @return a JCasIterator
   */
  def mainPipeline(): JCasIterator = iteratePipeline(
    reader,
    createEngineDescription(classOf[OpenNlpSegmenter],
      OpenNlpSegmenter.PARAM_TOKENIZATION_MODEL_LOCATION, SEGMENTER_DE_TOKEN_MODEL,
      OpenNlpSegmenter.PARAM_SEGMENTATION_MODEL_LOCATION, SEGMENTER_DE_SENTENCE_MODEL,
      OpenNlpSegmenter.PARAM_LANGUAGE, LANGUAGE),
    createEngineDescription(classOf[TokenTrimmer],
      TokenTrimmer.PARAM_PREFIXES, Array(":", "\"", ".", "|", "“", "„", "-", "_", " ", "—", "–", "-", "\u00AD", "‚", "‘", "?", "?", "…", "!", ";", "(", "(", "(", ")", ")", ")", ")"),
      TokenTrimmer.PARAM_SUFFIXES, Array()),
    createEngineDescription(classOf[TrailingCharacterRemover],
      TrailingCharacterRemover.PARAM_MIN_TOKEN_LENGTH, 1,
      TrailingCharacterRemover.PARAM_PATTERN, "[\\Q,‚‘.:|_„\u00AD--–??! ;“^»*’…((()))&/\"'©§'_—«·=\\E0-9]+"),
    createEngineDescription(classOf[ReadingTimeEstimator]),
    createEngineDescription(classOf[StopWordRemover],
      StopWordRemover.PARAM_MODEL_LOCATION, STOPWORD_FILE),
    createEngineDescription(classOf[OpenNlpPosTagger],
      OpenNlpPosTagger.PARAM_MODEL_LOCATION, POS_TAGGER_DE_MODEL,
      OpenNlpPosTagger.PARAM_LANGUAGE, LANGUAGE),
    createEngineDescription(classOf[IxaLemmatizer],
      IxaLemmatizer.PARAM_MODEL_ARTIFACT_URI, LEMMATIZER_DE_MODEL,
      IxaLemmatizer.PARAM_LANGUAGE, LANGUAGE),
    createEngineDescription(classOf[TfIdfCalculator]),
    createEngineDescription(classOf[JsonWriter])
  ).iterator()

}


object Corpus {

  /**
   * Creates the Corpus with the two reader descriptions needed to run the pipeline.
   *
   * @return Corpus
   */
  def fromDb(): Corpus = {
    Corpus(createReaderDescription(
      classOf[ReaderSecondPipeline]),
      createReaderDescription(
        classOf[ReaderFirstPipeline]))
  }
}