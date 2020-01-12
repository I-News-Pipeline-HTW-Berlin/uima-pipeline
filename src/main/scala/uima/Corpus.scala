package uima

import db.{JSONReaderDB, JSONReaderDbForFirstPipeline}
import de.tudarmstadt.ukp.dkpro.core.api.io.ResourceCollectionReaderBase
import de.tudarmstadt.ukp.dkpro.core.corenlp.CoreNlpNamedEntityRecognizer
import de.tudarmstadt.ukp.dkpro.core.ixa.IxaLemmatizer
import de.tudarmstadt.ukp.dkpro.core.opennlp.{OpenNlpNamedEntityRecognizer, OpenNlpPosTagger, OpenNlpSegmenter}
import de.tudarmstadt.ukp.dkpro.core.stanfordnlp.StanfordNamedEntityRecognizer
import de.tudarmstadt.ukp.dkpro.core.stopwordremover.StopWordRemover
import org.apache.uima.collection.CollectionReaderDescription
import org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription
import org.apache.uima.fit.factory.CollectionReaderFactory.createReaderDescription
import org.apache.uima.fit.pipeline.JCasIterator
import org.apache.uima.fit.pipeline.SimplePipeline.iteratePipeline
import de.tudarmstadt.ukp.dkpro.core.textnormalizer.annotations.TrailingCharacterRemover
import de.tudarmstadt.ukp.dkpro.core.tokit.TokenTrimmer

case class Corpus(reader: CollectionReaderDescription, readerForModel: CollectionReaderDescription) {

  val STOPWORD_FILE = "src/main/resources/stopwords-de.txt"
  val POS_TAGGER_DE_MODEL = "src/main/resources/de-pos-maxent.bin"
  val SEGMENTER_DE_TOKEN_MODEL = "src/main/resources/de-token.bin"
  val SEGMENTER_DE_SENTENCE_MODEL = "src/main/resources/de-sent.bin"
  //val NAMED_ENTITY_RECOGNIZER_MODEL = "src/main/resources/nemgp_stanford_01"
  val NAMED_ENTITY_RECOGNIZER_MODEL = "src/main/resources/nemgp_opennlp_01.bin"

  def tokenize(): JCasIterator =
    iteratePipeline(
      reader,
      createEngineDescription(classOf[OpenNlpSegmenter],
        OpenNlpSegmenter.PARAM_TOKENIZATION_MODEL_LOCATION, SEGMENTER_DE_TOKEN_MODEL,
        OpenNlpSegmenter.PARAM_SEGMENTATION_MODEL_LOCATION, SEGMENTER_DE_SENTENCE_MODEL,
        OpenNlpSegmenter.PARAM_LANGUAGE, "de")
    ).iterator()

  def removeStopWords(): JCasIterator =
    iteratePipeline(
      reader,
      createEngineDescription(classOf[OpenNlpSegmenter],
        OpenNlpSegmenter.PARAM_TOKENIZATION_MODEL_LOCATION, SEGMENTER_DE_TOKEN_MODEL,
        OpenNlpSegmenter.PARAM_SEGMENTATION_MODEL_LOCATION, SEGMENTER_DE_SENTENCE_MODEL,
        OpenNlpSegmenter.PARAM_LANGUAGE, "de"),
      createEngineDescription(classOf[StopWordRemover],
        StopWordRemover.PARAM_MODEL_LOCATION, STOPWORD_FILE)
    ).iterator()

  def lemmatize(): JCasIterator =
    iteratePipeline(
      reader,
      createEngineDescription(classOf[OpenNlpSegmenter],
        OpenNlpSegmenter.PARAM_TOKENIZATION_MODEL_LOCATION, SEGMENTER_DE_TOKEN_MODEL,
        OpenNlpSegmenter.PARAM_SEGMENTATION_MODEL_LOCATION, SEGMENTER_DE_SENTENCE_MODEL,
        OpenNlpSegmenter.PARAM_LANGUAGE, "de"),
      createEngineDescription(classOf[StopWordRemover],
        StopWordRemover.PARAM_MODEL_LOCATION, STOPWORD_FILE),
      createEngineDescription(classOf[OpenNlpPosTagger],
        OpenNlpPosTagger.PARAM_MODEL_LOCATION, POS_TAGGER_DE_MODEL,
        OpenNlpPosTagger.PARAM_LANGUAGE, "de"),
      /*createEngineDescription(classOf[OpenNlpLemmatizer], OpenNlpLemmatizer.PARAM_MODEL_LOCATION, MODEL_GERMAN,
        OpenNlpLemmatizer.PARAM_LANGUAGE, "de")*/
      //TODO find better lemmatizer with german model available
      //createEngineDescription(classOf[LanguageToolLemmatizer])
      createEngineDescription(classOf[IxaLemmatizer],
        IxaLemmatizer.PARAM_MODEL_ARTIFACT_URI, "mvn:de.tudarmstadt.ukp.dkpro.core:de.tudarmstadt.ukp.dkpro.core.ixa-model-lemmatizer-de-perceptron-conll09:20160213.1",
        IxaLemmatizer.PARAM_LANGUAGE, "de")
    ).iterator()

  def estimateReadingTime(): JCasIterator = iteratePipeline(
    reader,
    createEngineDescription(classOf[OpenNlpSegmenter],
      OpenNlpSegmenter.PARAM_TOKENIZATION_MODEL_LOCATION, SEGMENTER_DE_TOKEN_MODEL,
      OpenNlpSegmenter.PARAM_SEGMENTATION_MODEL_LOCATION, SEGMENTER_DE_SENTENCE_MODEL,
      OpenNlpSegmenter.PARAM_LANGUAGE, "de"),
    createEngineDescription(classOf[ReadingTimeEstimator],
      ReadingTimeEstimator.WORDS_PER_MINUTE, "200.0")
  ).iterator()

  def testPipeline(): JCasIterator = iteratePipeline(
    reader,
    createEngineDescription(classOf[OpenNlpSegmenter],
      OpenNlpSegmenter.PARAM_TOKENIZATION_MODEL_LOCATION, SEGMENTER_DE_TOKEN_MODEL,
      OpenNlpSegmenter.PARAM_SEGMENTATION_MODEL_LOCATION, SEGMENTER_DE_SENTENCE_MODEL,
      OpenNlpSegmenter.PARAM_LANGUAGE, "de"),
    createEngineDescription(classOf[TokenTrimmer],
      TokenTrimmer.PARAM_PREFIXES, Array(":", "\"", ".", "|", "“", "„", "-", "_", "—", "–", "\u00AD", "‚", "‘", "?", "?", "…", "!", ";", "(", "(", "(", ")", ")", ")",")"),
      TokenTrimmer.PARAM_SUFFIXES, Array()),
    createEngineDescription(classOf[TrailingCharacterRemover],
      TrailingCharacterRemover.PARAM_MIN_TOKEN_LENGTH, 1,
      TrailingCharacterRemover.PARAM_PATTERN, "[\\Q,‚‘.:|_„\u00AD-–??!;“^»*’…((()))&/\"'©§'—«·=\\E0-9]+"),
    createEngineDescription(classOf[ReadingTimeEstimator],
      ReadingTimeEstimator.WORDS_PER_MINUTE, "200.0"),
    createEngineDescription(classOf[StopWordRemover],
      StopWordRemover.PARAM_MODEL_LOCATION, STOPWORD_FILE),
    createEngineDescription(classOf[OpenNlpPosTagger],
      OpenNlpPosTagger.PARAM_MODEL_LOCATION, POS_TAGGER_DE_MODEL,
      OpenNlpPosTagger.PARAM_LANGUAGE, "de"),
    /*createEngineDescription(classOf[OpenNlpLemmatizer], OpenNlpLemmatizer.PARAM_MODEL_LOCATION, MODEL_GERMAN,
      OpenNlpLemmatizer.PARAM_LANGUAGE, "de")*/
    //TODO find better lemmatizer with german model available
    //createEngineDescription(classOf[LanguageToolLemmatizer])
    createEngineDescription(classOf[IxaLemmatizer],
      IxaLemmatizer.PARAM_MODEL_ARTIFACT_URI, "mvn:de.tudarmstadt.ukp.dkpro.core:de.tudarmstadt.ukp.dkpro.core.ixa-model-lemmatizer-de-perceptron-conll09:20160213.1",
      IxaLemmatizer.PARAM_LANGUAGE, "de"),
    createEngineDescription(classOf[TfIdfCalculator],
      TfIdfCalculator.MODEL_PATH, "src/main/resources/idfmodel.json",
      TfIdfCalculator.PERCENT_OF_LEMMAS, "0.0285"),
    createEngineDescription(classOf[JsonWriter],
      JsonWriter.DEPARTMENTS_PATH, "src/main/resources/departments.json")
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
        TokenTrimmer.PARAM_PREFIXES, Array(":", "\"", ".", "|", "“", "„", "-", "_", "—", "–", "\u00AD", "‚", "‘", "?", "?", "…", "!", ";", "(", "(", "(", ")", ")", ")",")"),
        TokenTrimmer.PARAM_SUFFIXES, Array()),
      createEngineDescription(classOf[TrailingCharacterRemover],
        TrailingCharacterRemover.PARAM_MIN_TOKEN_LENGTH, 1,
        TrailingCharacterRemover.PARAM_PATTERN, "[\\Q,‚‘.:|_„\u00AD-–??!;“^»*’…((()))&/\"'©§'—«·=\\E0-9]+"),

      //findet viel, aber ist nur teilweise korrekt
      /*createEngineDescription(classOf[StanfordNamedEntityRecognizer],
        StanfordNamedEntityRecognizer.PARAM_LANGUAGE, "de",
        StanfordNamedEntityRecognizer.PARAM_MODEL_LOCATION, NAMED_ENTITY_RECOGNIZER_MODEL),*/

      // findet viel und ist am korrektesten, nur problem bei namen: vorname und nachname werden als namen erkannt, aber nicht als 1 name
      /*createEngineDescription(classOf[CoreNlpNamedEntityRecognizer],
        CoreNlpNamedEntityRecognizer.PARAM_LANGUAGE, "de")*/

      // findet insgesamt nur sehr wenige named entities, daher nicht so gut
      createEngineDescription(classOf[OpenNlpNamedEntityRecognizer],
        OpenNlpNamedEntityRecognizer.PARAM_LANGUAGE, "de",
        OpenNlpNamedEntityRecognizer.PARAM_MODEL_LOCATION, NAMED_ENTITY_RECOGNIZER_MODEL),
      createEngineDescription(classOf[StopWordRemover],
        StopWordRemover.PARAM_MODEL_LOCATION, STOPWORD_FILE),
      createEngineDescription(classOf[OpenNlpPosTagger],
        OpenNlpPosTagger.PARAM_MODEL_LOCATION, POS_TAGGER_DE_MODEL,
        OpenNlpPosTagger.PARAM_LANGUAGE, "de"),
      createEngineDescription(classOf[IxaLemmatizer],
        IxaLemmatizer.PARAM_MODEL_ARTIFACT_URI, "mvn:de.tudarmstadt.ukp.dkpro.core:de.tudarmstadt.ukp.dkpro.core.ixa-model-lemmatizer-de-perceptron-conll09:20160213.1",
        IxaLemmatizer.PARAM_LANGUAGE, "de"),

      createEngineDescription(classOf[IdfDictionaryCreator],
        IdfDictionaryCreator.MODEL_PATH, "src/main/resources/idfmodel.json")
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

  def fromDb(userName: String, pw: String, serverAddress: String, port: String, db: String, collectionName: String, fileLocation: String): Corpus = {

    Corpus(createReaderDescription(
      classOf[JSONReaderDB],
      JSONReaderDB.USER_NAME, userName,
      JSONReaderDB.PW, pw,
      JSONReaderDB.SERVER_ADDRESS, serverAddress,
      JSONReaderDB.PORT, port,
      JSONReaderDB.DB, db,
      JSONReaderDB.COLLECTION_NAME, collectionName,
      JSONReaderDB.FILE_LOCATION, fileLocation),
      createReaderDescription(
      classOf[JSONReaderDbForFirstPipeline],
        JSONReaderDbForFirstPipeline.USER_NAME, userName,
        JSONReaderDbForFirstPipeline.PW, pw,
        JSONReaderDbForFirstPipeline.SERVER_ADDRESS, serverAddress,
        JSONReaderDbForFirstPipeline.PORT, port,
        JSONReaderDbForFirstPipeline.DB, db,
        JSONReaderDbForFirstPipeline.COLLECTION_NAME, collectionName,
        JSONReaderDbForFirstPipeline.FILE_LOCATION, fileLocation))
  }
}