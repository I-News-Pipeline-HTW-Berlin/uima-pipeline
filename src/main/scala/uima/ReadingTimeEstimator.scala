package uima

import com.typesafe.config.ConfigFactory
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.`type`.Token
import org.apache.uima.fit.component.JCasAnnotator_ImplBase
import org.apache.uima.fit.descriptor.ConfigurationParameter
import org.apache.uima.fit.util.JCasUtil
import org.apache.uima.jcas.JCas
import de.tudarmstadt.ukp.dkpro.core.api.metadata.`type`.MetaDataStringField




class ReadingTimeEstimator extends JCasAnnotator_ImplBase{

  //@ConfigurationParameter(name = ReadingTimeEstimator.WORDS_PER_MINUTE)
  val wordsPerMinute: String = ConfigFactory.load().getString("app.wordsperminute")

  def estimateReadingTime(wordCount: Int, wordsPerMinute: Double = 200.0) : Int = {
    val estimateTime = wordCount.toDouble / wordsPerMinute
    val minutes = estimateTime.toInt
    val seconds = (estimateTime - minutes) * 0.60
    seconds match {
      case x if x < 0.3 => minutes
      case _ => minutes + 1
    }
  }


  override def process(aJCas: JCas): Unit = {
    val numOfWords = JCasUtil.select(aJCas, classOf[Token]).size()
    val readingTime = estimateReadingTime(numOfWords, wordsPerMinute.toDouble)
    val metaDataStringField = new MetaDataStringField(aJCas, 0, numOfWords-1)
    metaDataStringField.setKey("readingTime")
    metaDataStringField.setValue(readingTime.toString)
    metaDataStringField.addToIndexes()
  }
}

/*object ReadingTimeEstimator{
  final val WORDS_PER_MINUTE = "wordsPerMinute"
}*/