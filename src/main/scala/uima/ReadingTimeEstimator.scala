package uima

import com.typesafe.config.ConfigFactory
import de.tudarmstadt.ukp.dkpro.core.api.metadata.`type`.MetaDataStringField
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.`type`.Token
import org.apache.uima.fit.component.JCasAnnotator_ImplBase
import org.apache.uima.fit.util.JCasUtil
import org.apache.uima.jcas.JCas

/**
 * This component calculates the time it takes to read the text of each document.
 */
class ReadingTimeEstimator extends JCasAnnotator_ImplBase{

  /**
   * The number of words the average reader can read in one minute (assumption).
   */
  val wordsPerMinute: String = ConfigFactory.load().getString("app.wordsperminute")

  /**
   * Estimates the reading time and rounds it up or down to full minutes.
   * @param wordCount
   * @param wordsPerMinute
   * @return Int (minutes)
   */
  def estimateReadingTime(wordCount: Int, wordsPerMinute: Double = 200.0) : Int = {
    val estimateTime = wordCount.toDouble / wordsPerMinute
    val minutes = estimateTime.toInt
    val seconds = (estimateTime - minutes) * 0.60
    seconds match {
      case x if x < 0.3 => minutes
      case _ => minutes + 1
    }
  }

  /**
   * Gets the number of words of document text,
   * calculates reading time and
   * uses MetaDataStringField annotation to save reading time for document.
   * @param aJCas
   */
  override def process(aJCas: JCas): Unit = {
    val numOfWords = JCasUtil.select(aJCas, classOf[Token]).size()
    val readingTime = estimateReadingTime(numOfWords, wordsPerMinute.toDouble)
    val metaDataStringField = new MetaDataStringField(aJCas, 0, numOfWords-1)
    metaDataStringField.setKey("readingTime")
    metaDataStringField.setValue(readingTime.toString)
    metaDataStringField.addToIndexes()
  }
}
