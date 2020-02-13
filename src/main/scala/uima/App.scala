package uima

object App {

  def main(args: Array[String]) {

    val corpus = Corpus.fromDb()

    /**
     * Execution of first pipeline,
     * calculates IDF-model
     */
    val modelIt = corpus.writeModel()
    modelIt.forEachRemaining(jcas => {})

    /**
     * Execution of second pipeline,
     * calculates reading time, tfidf, most relevant lemmas, lemmas and writes outcome to db
     */
    val mainPipeIt = corpus.mainPipeline()
    mainPipeIt.forEachRemaining(jcas => {})

  }
}
