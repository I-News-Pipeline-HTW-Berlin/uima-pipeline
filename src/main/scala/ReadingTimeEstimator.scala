import de.tudarmstadt.ukp.dkpro.core.api.parameter.ComponentParameters
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.`type`.Token
import org.apache.uima.cas.{Feature, FeatureStructure, Type}
import org.apache.uima.fit.component.JCasAnnotator_ImplBase
import org.apache.uima.fit.descriptor.ConfigurationParameter
import org.apache.uima.fit.pipeline.JCasIterator
import org.apache.uima.fit.util.JCasUtil
import org.apache.uima.jcas.JCas


class ReadingTimeEstimator extends JCasAnnotator_ImplBase{

  @ConfigurationParameter(name = ReadingTimeEstimator.WORDS_PER_MINUTE)
  val wordsPerMinute = "200.0"

  def estimateReadingTime(wordCount: Int, wordsPerMinute: Double = 200.0) : Int = {
    /*val corpus = Corpus.fromDir("testResourcesJSON")
    val jcasIterator = corpus.tokenize()
    jcasIterator.forEachRemaining(jcas => {
      print("\n\n")
      val tokens = JCasUtil.select(jcas, classOf[Token])
      val wordCount = tokens.size()*/
    //println("wordCount: "+wordCount+ " wpm: "+wordsPerMinute)
    val estimateTime = wordCount.toDouble / wordsPerMinute
    //println("estimate time: "+estimateTime)
    val minutes = estimateTime.toInt
    //println("minutes: "+minutes)
    val seconds = (estimateTime - minutes) * 0.60
    //println("seconds: "+seconds)
    seconds match {
      case x if x < 0.3 => minutes
      case _ => minutes + 1
    }
  }


  override def process(aJCas: JCas): Unit = {
    val numOfWords = JCasUtil.select(aJCas, classOf[Token]).size()
    val readingTime = estimateReadingTime(numOfWords, ReadingTimeEstimator.WORDS_PER_MINUTE.toDouble)
    //aJCas.createView("MetaView").setSofaDataString(readingTime.toString, String)
    val fakeToken = new Token(aJCas, 0, numOfWords-1)
    //print(readingTime)
    fakeToken.setText(String.valueOf(readingTime))
    fakeToken.addToIndexes()
  }
}

object ReadingTimeEstimator{
  final val WORDS_PER_MINUTE = "200.0"
}