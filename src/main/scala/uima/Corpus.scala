package uima

import db.{DbConnector, JSONReaderDB}
import de.tudarmstadt.ukp.dkpro.core.api.io.ResourceCollectionReaderBase
import de.tudarmstadt.ukp.dkpro.core.ixa.IxaLemmatizer
import de.tudarmstadt.ukp.dkpro.core.opennlp.{OpenNlpPosTagger, OpenNlpSegmenter}
import de.tudarmstadt.ukp.dkpro.core.stopwordremover.StopWordRemover
import org.apache.uima.collection.CollectionReaderDescription
import org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription
import org.apache.uima.fit.factory.CollectionReaderFactory.createReaderDescription
import org.apache.uima.fit.pipeline.JCasIterator
import org.apache.uima.fit.pipeline.SimplePipeline.iteratePipeline
import org.mongodb.scala.ServerAddress


case class Corpus(reader: CollectionReaderDescription) {

  val STOPWORD_FILE = "src/main/resources/stopwords-de.txt"
  val POS_TAGGER_DE_MODEL = "src/main/resources/de-pos-maxent.bin"
  val SEGMENTER_DE_TOKEN_MODEL = "src/main/resources/de-token.bin"
  val SEGMENTER_DE_SENTENCE_MODEL = "src/main/resources/de-sent.bin"

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
    createEngineDescription(classOf[ReadingTimeEstimator],
      ReadingTimeEstimator.WORDS_PER_MINUTE, "200.0"),
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
    createEngineDescription(classOf[JsonCollectionWriter])
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
      ResourceCollectionReaderBase.PARAM_LANGUAGE, lang
    ))
  }

  def fromDb(userName: String, pw: String, serverAddress: String, port: String, db: String, collectionName: String): Corpus = {

    Corpus(createReaderDescription(
      classOf[JSONReaderDB],
      JSONReaderDB.USER_NAME, userName,
      JSONReaderDB.PW, pw,
      JSONReaderDB.SERVER_ADDRESS, serverAddress,
      JSONReaderDB.PORT, port,
      JSONReaderDB.DB, db,
      JSONReaderDB.COLLECTION_NAME, collectionName
    ))
  }
}