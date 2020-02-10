# Uima-Pipeline
UIMA pipeline for I-News-Pipeline project. Based on DKPro Core, written in Scala

## Pipeline versions
At the moment we have two different branches. On the master we have the version that is currently running on the server. On branch 'spark' we have a version running with ApacheSpark that also contains two pipeline engines called NamedEntityRecognizer and NamedEntityMapper. The second is a working version, but as the NamedEntity components require a lot more time to process we didn't use it until now (update!).

## Pipeline results

The UIMA pipeline takes the scraped articles from the mongoDB, processes them and saves them again in a new collection. Currently, the final analysis contains the following:
* most relevant words for a given article (used as tags in our Frontend)
* estimated reading time for each article
* mapping of global newspaper departments for each article (i.e. politics, economics)

## Architecture of our pipeline

On a large scale, our pipeline is divided into two parts. The reason for this structure is due to our tf-idf model - a statistical measure often used in information retrieval and text mining. With the tf-idf weight we calculate the most relevant words, later used as tags, per article. IDF stands for 'Inverse Document Frequency' and TF stands for 'term frequency'. How the tf-idf weight is calculated:

* t = a given term in a article
* TF(t) = Number of times term t appears in a document / Total number of terms in the document
* IDF(t) = Total number of documents / Number of documents with term t in it
* TF IDF(t) = TF(t) * IDF(t)

As the IDF value needs to be calculated in respect to the overall number of documents, we decided to create a JSON file containing all the idf values for every term occuring in the articles so far, and in the second pipeline multiplying them with the tf value for the given term. 

The downside of our approach is that most of the processing steps (tokenizing, removing stopwords and punctuation etc.) have to be done two times.

### The overall processing steps in our pipeline are:

The following steps are done for each article separately coming from our mongoDB.

#### Pipeline I:
* JSON Reader: read in the scrapped articles from the mongoDB
* Segmenter: split each article first into sentences and then into individual tokens
* Removal of numbers and punctuation: [0-9], ".", "|", "“", "„", "-" etc.
* StopWordRemover: removes words such as "auf", "bei", "da" etc.
* POS Tagging: part of speech tagging; marks terms as noun, adverb, verb etc.
* Lemmatizer: lemmatizes all terms, for instance "vergaß" -> "vergessen"
* IDF calculator: calculates the IDF values for each term per article

#### Pipeline II:
* JSON Reader (TODO?)
* Segmenter
* Removal of numbers and punctuation
* NamedEntityRecognizer: automatic identification and classification of proper nouns, e.g. (Steinmeier, PERSON); currently identified persons are added to the most relevant words, extending the tags for the respective article. The same could be done for identified organisations or places if wished (only on branch spark)
* NamedEntityMapper (only on branch spark) (TODO?)
* ReadingTimeEstimator
* StopwordRemover
* POS Tagging
* Lemmatizer
* TfIdfCalculator: now the model created in the first pipeline is used tseparato calculate the final tf-idf weights
* JSON Writer: writes newly analysed article with attached departments, most relevant tags etc. to a new collection in the mongoDB

## Running the pipeline on the INews server

Connect to the server via ssh: ssh local@news.f4.htw-berlin.de
Change from local to root: su -
Project folder: home/uima/uima-pipeline

On the server we have two files 'inews_uima.service' and 'inews_uima.timer' located at /etc/systemd/system.
In the service file, amongst other things, we specify our working directory and the command how to execute our pipeline. Currently the program will be started with 'sbt run'. This was a workaround as we couldn't create a jar-file from our project due to some DKPro components that could not be correctly added to our dependencies. (TODO? richtig so?)
With 'Wants=inews_uima.timer' we provide the connection to our .timer file. In the latter, we define the date/ time when the server should run our program. Currently it is set to be executed each day at 01:00:00.

In our project folder under src/main/resources the 'application.conf' file contains various configuration parameters such as the server name, the database and collection names where the scraped articles are taken from and after processing written to, usernames, passwords etc.
Further, we define the location of the following files: file containing the last crawl time, our idfmodel, our departments mapping file.
We also define the percentage of how many most relevant terms (tags) we want to save from a given article (longer articles -> more relevant terms/ shorter articles -> less terms) and the number of words an average person could read per minute (used to estimate the reading time per article).




# Further links:
* UserGuide for ApacheFit: https://uima.apache.org/d/uimafit-current/tools.uimafit.book.html
* DKPro User Guide: https://dkpro.github.io/dkpro-core/releases/2.0.0/docs/user-guide.html
* DKPro Developer Guide: https://dkpro.github.io/dkpro-core/releases/2.0.0/docs/developer-guide.html
