package uima

import db.{JSONReaderDB, JSONReaderDbForFirstPipeline}
import de.tudarmstadt.ukp.dkpro.core.api.io.ResourceCollectionReaderBase
import de.tudarmstadt.ukp.dkpro.core.corenlp.CoreNlpNamedEntityRecognizer
import de.tudarmstadt.ukp.dkpro.core.ixa.IxaLemmatizer
import de.tudarmstadt.ukp.dkpro.core.opennlp.{OpenNlpPosTagger, OpenNlpSegmenter}
import de.tudarmstadt.ukp.dkpro.core.stopwordremover.StopWordRemover
import org.apache.uima.collection.CollectionReaderDescription
import org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription
import org.apache.uima.fit.factory.CollectionReaderFactory.createReaderDescription
import org.apache.uima.fit.pipeline.JCasIterator
import org.apache.uima.fit.pipeline.SimplePipeline.iteratePipeline
import de.tudarmstadt.ukp.dkpro.core.textnormalizer.annotations.TrailingCharacterRemover
import de.tudarmstadt.ukp.dkpro.core.tokit.TokenTrimmer
import com.typesafe.config.ConfigFactory

case class Corpus(reader: CollectionReaderDescription, readerForModel: CollectionReaderDescription) {

  val STOPWORD_FILE = ConfigFactory.load().getString("app.stopwordfile")
  val POS_TAGGER_DE_MODEL = ConfigFactory.load().getString("app.postaggermodel")
  val SEGMENTER_DE_TOKEN_MODEL = ConfigFactory.load().getString("app.segmentertokenmodel")
  val SEGMENTER_DE_SENTENCE_MODEL = ConfigFactory.load().getString("app.segmentersentencemodel")
  val language = ConfigFactory.load().getString("app.language")
  val lemmaModel = ConfigFactory.load().getString("app.lemmatizermodelartifacturi")
  val charactersToRemove = Array(":", "\"", ".", "|", "“", "„", "-", "_", "—", "–", "\u00AD", "‚", "‘", "?", "?", "…",
    "!", ";", "(", "(", "(", ")", ")", ")",")", "[", "\\", ",", "‚", "‘", ".", ":", "|", "_", "„", "-", "-", "–", " ",
    ";", "“", "^", "»", "*", "’", "&", "/", "\\", "\"", "'", "©", "§", "'", "—", "«", "·", "=", "\\", "+", "“",
    "0", "1", "2", "3", "4", "5", "6", "7", "8", "9")
  //val NAMED_ENTITY_RECOGNIZER_MODEL = "src/main/resources/nemgp_stanford_01"
  //val NAMED_ENTITY_RECOGNIZER_MODEL = "src/main/resources/nemgp_opennlp_01.bin"
  //val NAMED_ENTITY_RECOGNIZER_MODEL_LOCATION = "src/main/resources/de.tudarmstadt.ukp.dkpro.core.stanfordnlp-model-ner-de-germeval2014.hgc_175m_600.crf-20180227.1/de/tudarmstadt/ukp/dkpro/core/stanfordnlp/lib/ner-de-germeval2014.hgc_175m_600.crf.properties"
  //val NAMED_ENTITY_RECOGNIZER_MODEL_LOCATION = "src/main/resources/de.tudarmstadt.ukp.dkpro.core.stanfordnlp-model-ner-de-germeval2014.hgc_175m_600.crf-20180227.1"
  //val NAMED_ENTITY_RECOGNIZER_MODEL_LOCATION = "src/main/resources/de.tudarmstadt.ukp.dkpro.core.stanfordnlp-model-ner-de-germeval2014.hgc_175m_600.crf-20180227.1.pom"

  def tokenize(): JCasIterator =
    iteratePipeline(
      reader,
      createEngineDescription(classOf[OpenNlpSegmenter],
        OpenNlpSegmenter.PARAM_TOKENIZATION_MODEL_LOCATION, SEGMENTER_DE_TOKEN_MODEL,
        OpenNlpSegmenter.PARAM_SEGMENTATION_MODEL_LOCATION, SEGMENTER_DE_SENTENCE_MODEL,
        OpenNlpSegmenter.PARAM_LANGUAGE, language)
    ).iterator()

  def removeStopWords(): JCasIterator =
    iteratePipeline(
      reader,
      createEngineDescription(classOf[OpenNlpSegmenter],
        OpenNlpSegmenter.PARAM_TOKENIZATION_MODEL_LOCATION, SEGMENTER_DE_TOKEN_MODEL,
        OpenNlpSegmenter.PARAM_SEGMENTATION_MODEL_LOCATION, SEGMENTER_DE_SENTENCE_MODEL,
        OpenNlpSegmenter.PARAM_LANGUAGE, language),
      createEngineDescription(classOf[StopWordRemover],
        StopWordRemover.PARAM_MODEL_LOCATION, STOPWORD_FILE)
    ).iterator()

  def lemmatize(): JCasIterator =
    iteratePipeline(
      reader,
      createEngineDescription(classOf[OpenNlpSegmenter],
        OpenNlpSegmenter.PARAM_TOKENIZATION_MODEL_LOCATION, SEGMENTER_DE_TOKEN_MODEL,
        OpenNlpSegmenter.PARAM_SEGMENTATION_MODEL_LOCATION, SEGMENTER_DE_SENTENCE_MODEL,
        OpenNlpSegmenter.PARAM_LANGUAGE, language),
      createEngineDescription(classOf[StopWordRemover],
        StopWordRemover.PARAM_MODEL_LOCATION, STOPWORD_FILE),
      createEngineDescription(classOf[OpenNlpPosTagger],
        OpenNlpPosTagger.PARAM_MODEL_LOCATION, POS_TAGGER_DE_MODEL,
        OpenNlpPosTagger.PARAM_LANGUAGE, language),
      /*createEngineDescription(classOf[OpenNlpLemmatizer], OpenNlpLemmatizer.PARAM_MODEL_LOCATION, MODEL_GERMAN,
        OpenNlpLemmatizer.PARAM_LANGUAGE, "de")*/
      //TODO find better lemmatizer with german model available
      //createEngineDescription(classOf[LanguageToolLemmatizer])
      createEngineDescription(classOf[IxaLemmatizer],
        IxaLemmatizer.PARAM_MODEL_ARTIFACT_URI, lemmaModel,
        IxaLemmatizer.PARAM_LANGUAGE, language)
    ).iterator()

  def estimateReadingTime(): JCasIterator = iteratePipeline(
    reader,
    createEngineDescription(classOf[OpenNlpSegmenter],
      OpenNlpSegmenter.PARAM_TOKENIZATION_MODEL_LOCATION, SEGMENTER_DE_TOKEN_MODEL,
      OpenNlpSegmenter.PARAM_SEGMENTATION_MODEL_LOCATION, SEGMENTER_DE_SENTENCE_MODEL,
      OpenNlpSegmenter.PARAM_LANGUAGE, language),
    createEngineDescription(classOf[ReadingTimeEstimator])
  ).iterator()

  def mainPipeline(): JCasIterator = iteratePipeline(
    reader,
    createEngineDescription(classOf[OpenNlpSegmenter],
      OpenNlpSegmenter.PARAM_TOKENIZATION_MODEL_LOCATION, SEGMENTER_DE_TOKEN_MODEL,
      OpenNlpSegmenter.PARAM_SEGMENTATION_MODEL_LOCATION, SEGMENTER_DE_SENTENCE_MODEL,
      OpenNlpSegmenter.PARAM_LANGUAGE, language),
    createEngineDescription(classOf[TokenTrimmer],
      TokenTrimmer.PARAM_PREFIXES, charactersToRemove,
      TokenTrimmer.PARAM_SUFFIXES, charactersToRemove),
    /*createEngineDescription(classOf[TrailingCharacterRemover],
      TrailingCharacterRemover.PARAM_MIN_TOKEN_LENGTH, 1,
      TrailingCharacterRemover.PARAM_PATTERN, "[\\Q,‚‘.:|_„\u00AD--–??! ;“^»*’…((()))&/\"'©§'—«·=\\E0-9]+"),*/
    createEngineDescription(classOf[CoreNlpNamedEntityRecognizer],
      CoreNlpNamedEntityRecognizer.PARAM_LANGUAGE, language),
    createEngineDescription(classOf[NamedEntityMapper]),
    createEngineDescription(classOf[ReadingTimeEstimator]),
    createEngineDescription(classOf[CoreNlpNamedEntityRecognizer],
      CoreNlpNamedEntityRecognizer.PARAM_LANGUAGE, language/*,
      CoreNlpNamedEntityRecognizer.PARAM_MODEL_LOCATION, NAMED_ENTITY_RECOGNIZER_MODEL_LOCATION*/),
    createEngineDescription(classOf[StopWordRemover],
      StopWordRemover.PARAM_MODEL_LOCATION, STOPWORD_FILE),
    createEngineDescription(classOf[OpenNlpPosTagger],
      OpenNlpPosTagger.PARAM_MODEL_LOCATION, POS_TAGGER_DE_MODEL,
      OpenNlpPosTagger.PARAM_LANGUAGE, language),
    /*createEngineDescription(classOf[OpenNlpLemmatizer], OpenNlpLemmatizer.PARAM_MODEL_LOCATION, MODEL_GERMAN,
      OpenNlpLemmatizer.PARAM_LANGUAGE, "de")*/
    //TODO find better lemmatizer with german model available
    //createEngineDescription(classOf[LanguageToolLemmatizer])
    createEngineDescription(classOf[IxaLemmatizer],
      IxaLemmatizer.PARAM_MODEL_ARTIFACT_URI, lemmaModel,
      IxaLemmatizer.PARAM_LANGUAGE, language),
    createEngineDescription(classOf[NumberAndPunctuationRemover]),
    createEngineDescription(classOf[TfIdfCalculator]),
    createEngineDescription(classOf[JsonWriter])
  ).iterator()

  def writeModel(): JCasIterator =
   /*val idfDictCreatorDesc = createEngineDescription(classOf[IdfDictionaryCreator],
      IdfDictionaryCreator.MODEL_PATH, "src/main/resources/idfmodel.json")
    val idfDictCreator = AnalysisEngineFactory.createEngine(idfDictCreatorDesc)*/
    iteratePipeline(
      readerForModel,
      createEngineDescription(classOf[OpenNlpSegmenter],
        OpenNlpSegmenter.PARAM_TOKENIZATION_MODEL_LOCATION, SEGMENTER_DE_TOKEN_MODEL,
        OpenNlpSegmenter.PARAM_SEGMENTATION_MODEL_LOCATION, SEGMENTER_DE_SENTENCE_MODEL,
        OpenNlpSegmenter.PARAM_LANGUAGE, "de"),
      createEngineDescription(classOf[TokenTrimmer],
        TokenTrimmer.PARAM_PREFIXES, charactersToRemove,
        TokenTrimmer.PARAM_SUFFIXES, charactersToRemove),
      /*createEngineDescription(classOf[TrailingCharacterRemover],
        TrailingCharacterRemover.PARAM_MIN_TOKEN_LENGTH, 1,
        TrailingCharacterRemover.PARAM_PATTERN, "[\\Q,‚‘.:|_„\u00AD-–??!;“^»*’…((()))&/\"'©§'—«·=\\E0-9]+"),*/

      //findet viel, aber ist nur teilweise korrekt
     /* createEngineDescription(classOf[StanfordNamedEntityRecognizer],
        StanfordNamedEntityRecognizer.PARAM_LANGUAGE, "de",
        StanfordNamedEntityRecognizer.PARAM_MODEL_LOCATION, NAMED_ENTITY_RECOGNIZER_MODEL),*/

      // findet viel und ist am korrektesten, nur problem bei namen: vorname und nachname werden als namen erkannt, aber nicht als 1 name
      // in zusammenarbeit mit dem mapper am besten
      createEngineDescription(classOf[CoreNlpNamedEntityRecognizer],
        CoreNlpNamedEntityRecognizer.PARAM_LANGUAGE, language/*,
        CoreNlpNamedEntityRecognizer.PARAM_MODEL_LOCATION, NAMED_ENTITY_RECOGNIZER_MODEL_LOCATION*/),
      createEngineDescription(classOf[NamedEntityMapper]),

      // findet insgesamt nur sehr wenige named entities, daher nicht so gut
      /*createEngineDescription(classOf[OpenNlpNamedEntityRecognizer],
        OpenNlpNamedEntityRecognizer.PARAM_LANGUAGE, "de",
        OpenNlpNamedEntityRecognizer.PARAM_MODEL_LOCATION, NAMED_ENTITY_RECOGNIZER_MODEL),*/
      createEngineDescription(classOf[StopWordRemover],
        StopWordRemover.PARAM_MODEL_LOCATION, STOPWORD_FILE),
      createEngineDescription(classOf[OpenNlpPosTagger],
        OpenNlpPosTagger.PARAM_MODEL_LOCATION, POS_TAGGER_DE_MODEL,
        OpenNlpPosTagger.PARAM_LANGUAGE, language),
      createEngineDescription(classOf[IxaLemmatizer],
        IxaLemmatizer.PARAM_MODEL_ARTIFACT_URI, lemmaModel,
        IxaLemmatizer.PARAM_LANGUAGE, language),
      createEngineDescription(classOf[NumberAndPunctuationRemover]),
      createEngineDescription(classOf[IdfDictionaryCreator])
    ).iterator()


}



object Corpus {
  def fromDir(directory: String, pattern: String = "[+]**/*.json", lang: String = "de"): Corpus = {
    /*uima.Corpus(createReaderDescription(
      classOf[TextReader],
      ResourceCollectionReaderBase.PARAM_SOURCE_LOCATION, directory,
      ResourceCollectionReaderBase.PARAM_PATTERNS, pattern,
      ResourceCollectionReaderBase.PARAM_LANGUAGE, lang
    ))*/

    Corpus(createReaderDescription(
      classOf[JSONReader],
      ResourceCollectionReaderBase.PARAM_SOURCE_LOCATION, directory,
      ResourceCollectionReaderBase.PARAM_PATTERNS, pattern,
      ResourceCollectionReaderBase.PARAM_LANGUAGE, lang),
      createReaderDescription(
      classOf[JSONReaderDbForFirstPipeline],
      ResourceCollectionReaderBase.PARAM_SOURCE_LOCATION, directory,
      ResourceCollectionReaderBase.PARAM_PATTERNS, pattern,
      ResourceCollectionReaderBase.PARAM_LANGUAGE, lang))
  }

  def fromDb(): Corpus = {

    Corpus(createReaderDescription(
      classOf[JSONReaderDB]),
      createReaderDescription(
      classOf[JSONReaderDbForFirstPipeline]))
  }
}