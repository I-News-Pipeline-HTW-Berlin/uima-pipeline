# Uima-Pipeline
UIMA pipeline for I-News-Pipeline project. Based on DKPro Core, written in Scala

This pipeline takes the scraped articles from the mongoDB, processes them and saves them again in a new collection. Currently, the analysis contains the following:
* most relevant words for a given article
* estimated reading time
* mapping of global newspaper departments

The necessary processing steps in the pipeline are:
* JSON Reader: read in the scrapped articles from the mongoDB
* Segmenter: split each article first into sentences and then into individual tokens
* Token trimmer: removes irrelevant tokens
* Number and punctuation remover:
* NamedEntityRecognizer
* Named EntityMapper
* ReadingTimeEstimator
* StopwordRemover
* Lemmatizer
* IdfModelWriter
* TfIdfCalculator
* JSON Writer: writes newly analysed article with attached departments, most relevant tags etc. to a new collection in the mongoDB





# Further links:
* UserGuide for ApacheFit: https://uima.apache.org/d/uimafit-current/tools.uimafit.book.html
* DKPro User Guide: https://dkpro.github.io/dkpro-core/releases/2.0.0/docs/user-guide.html
* DKPro Developer Guide: https://dkpro.github.io/dkpro-core/releases/2.0.0/docs/developer-guide.html
