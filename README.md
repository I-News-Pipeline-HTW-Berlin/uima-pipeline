# Uima-Pipeline
UIMA pipeline for I-News-Pipeline project. Based on DKPro Core, written in Scala

## Pipeline versions
At the moment we have two different branches. On the `master` we have the version that is currently running on the news server (every night 1 am). On branch `spark` we have a version running with ApacheSpark that also contains two analysis engines called NamedEntityRecognizer and NamedEntityMapper. The second is a working version, but as the NamedEntity components require a lot more time and memory to process it will take some more time until the analysis of the whole scraped_articles collection is done. It's currently running 4 times a day on hadoop05 server under the inews user id analysing 500 articles in each run.


## Pipeline results

The UIMA pipeline takes the latest scraped articles from the mongoDB, processes them and saves them again in a new collection. Currently, the final analysis contains the following:
* most relevant words for a given article (used as tags in our Frontend)
* estimated reading time for each article
* mapping of global newspaper departments for each article (i.e. politics, economics)


## Architecture of our pipeline

On a large scale, our pipeline is divided into two parts. The reason for this structure is due to our tf-idf model - a statistical measure often used in information retrieval and text mining. With the tf-idf weight we calculate the most relevant words, later used as tags, per article. IDF stands for 'Inverse Document Frequency' and TF stands for 'term frequency'. How the tf-idf weight is calculated:

```
t = a given term in a article
TF(t) = Number of times term t appears in a document / Total number of terms in the document
IDF(t) = Total number of documents / Number of documents with term t in it
TF IDF(t) = TF(t) * IDF(t)
```

As the IDF value needs to be calculated in respect to the overall number of documents, we decided to create a JSON file containing all the idf values for every term occuring in the articles so far, and in the second pipeline multiplying them with the tf value for the given term. Due to the architecture of UIMA Framework it is not possible to execute this in only one pipeline.

The downside of our approach is that most of the processing steps (tokenizing, removing stopwords and punctuation etc.) have to be done two times.

The advantage on the other side is that the tf-idf model is always up to date and tf-idf is calculated in respect to all previously analyzed documents.


### The overall processing steps in our pipeline are:

The following steps are done for each article separately coming from our mongoDB.

#### Pipeline I (writeModel pipeline):
* ReaderFirstPipeline: read the latest scrapped articles from the mongoDB and set the document text for JCas
* Segmenter: split each article first into sentences and then into individual tokens
* Removal of numbers (only on master) and punctuation: ".", "|", "“", "„", "-" etc.
* NamedEntityRecognizer: automatic identification and classification of proper nouns, e.g. (Steinmeier, PERSON); currently identified persons are added to the most relevant words later (if tf-idf for them is high enough), extending the tags for the respective article. The same could be done for identified organisations or places if wished (running only on branch spark)
* NamedEntityMapper: connects first and last name of all named entities with the value "PERSON" (running only on branch spark)
* StopWordRemover: removes words that are not meaningful by themselves such as "auf", "bei", "da" etc.
* POS Tagging: part of speech tagging; marks terms as noun, adverb, verb etc. (done by lemmatizer)
* Lemmatizer: lemmatizes all terms, for instance "vergaß" -> "vergessen"
* Removal of numbers: Removes "456" but not "i5-prozessor" (running only on branch spark, on master this is done earlier by TrailingCharacterRemover)
* IDF dictionary calculator: calculates the IDF values for each term (lemmas and named entities)

#### Pipeline II:
* ReaderSecondPipeline: read the latest scrapped articles from the mongoDB and set the document text for JCas, write last crawl time to file
* Segmenter
* Removal of numbers (only on master) and punctuation
* NamedEntityRecognizer (running only on branch spark)
* NamedEntityMapper (running only on branch spark)
* ReadingTimeEstimator
* StopwordRemover
* POS Tagging
* Lemmatizer 
* Removal of numbers (running only on branch spark, on master this is done earlier by TrailingCharacterRemover)
* TfIdfCalculator: now the model created in the first pipeline is used to calculate the final tf-idf weights and find most relevant lemmas
* JSON Writer: writes newly analysed article with attached departments, most relevant tags etc. to a new collection in the mongoDB


## Running the pipeline on the news server

- Connect to the server via ssh: `ssh local@news.f4.htw-berlin.de`
- Change from local to root: `su -`
- Project folder: `home/uima/uima-pipeline`

On the news server we use `systemd` as init system/ system manager to run our services. For that purpose we have two files `inews_uima.service` and `inews_uima.timer` located at `/etc/systemd/system`.
In the .service file, amongst other things, we specify our working directory and instructions for execution of our program. Currently it will be started with `sbt run`. This was a workaround as we couldn't create a jar-file from our project due to some DKPro components that could not be correctly shaded with sbt (this is a TODO). 
With `Wants=inews_uima.timer` we provide the connection to our .timer file. In the latter, we define the date/ time when the server should run our program. Currently it is set to be executed each day at 01:00:00.
Keep in mind that the running version on news server is the current version on master branch which does not include named entity tagging.

#### Starting and stoping systemd services
Note: If you are running as a non-root user, you will have to use `sudo` since this will affect the state of the operating system.

`systemctl start inews_uima.service` -> starts a systemd service, in this case the .service file
`systemctl stop inews_uima.service` -> stops a currently running service
`systemctl restart inews_uima.service` -> restarts a running service
`systemctl reload inews_uima.service` -> reloads the service
`systemctl daemon-reload` -> reloads systemd manager configuration, rerun all generators, reload all unit files, and recreate the dependency tree

#### Enabling and disabling services
The above commands are useful for starting or stopping commands during the current session. To tell systemd to start services automatically at boot, you must enable them.

`systemctl enable inews_uima.service` -> starts a service at boot
`systemctl disable inews_uima.service` -> disables the service from starting automatically

For further interest see for example here: https://www.digitalocean.com/community/tutorials/how-to-use-systemctl-to-manage-systemd-services-and-units or https://wiki.ubuntuusers.de/systemd/systemctl/


#### Check the status of a service
`systemctl status inews_uima.service` -> provides the service state and the first few log lines

## Running the pipeline on hadoop05 server

- Connect to the server via ssh: `ssh inews@hadoop05.f4.htw-berlin.de`
- Project folder: `~/uima-pipeline`

The code on branch spark ist currently running on hadoop05 as cron-job under the userid inews. Currently the code is being executed 4 times per day. Once the whole scraped_articles collection is analyzed this can be changed to only once a day at 1 am as done on news server. 
Currently the cron-job looks like this: `30 */6 * * * cd uima-pipeline && sbt run >>~/uima.log`.
To edit type `crontab -e`.

## Configuring the application

In our project folder under `src/main/resources` the `application.conf` file contains various configuration parameters such as the server name, the database and collection names where the scraped articles are taken from and after processing written to, usernames, passwords etc. Here you can also get information about the current mongodb target collections of each branch.  
Further, we define the location of the following files: file containing the last crawl time, our idfmodel, our departments mapping file.
We also define the percentage of how many most relevant terms (tags) we want to save from a given article (longer articles -> more relevant terms/ shorter articles -> less terms) and the number of words an average person could read per minute (used to estimate the reading time per article).
Further the project folder contains a `.jvmopts` file with memory specifications as the application is consuming a lot of memory.


## Further links:
* UserGuide for UIMAFit: https://uima.apache.org/d/uimafit-current/tools.uimafit.book.html
* DKPro User Guide: https://dkpro.github.io/dkpro-core/releases/2.0.0/docs/user-guide.html
* DKPro Developer Guide: https://dkpro.github.io/dkpro-core/releases/2.0.0/docs/developer-guide.html
