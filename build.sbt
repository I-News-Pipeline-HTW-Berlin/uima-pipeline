name := "DK_Pro"

version := "0.1"

/* scala 2.12 is necessary for spark */
scalaVersion := "2.12.10"

resolvers += "ukp-oss-model-releases" at "http://zoidberg.ukp.informatik.tu-darmstadt.de/artifactory/public-model-releases-local"

libraryDependencies ++= Seq(
  /* DKPro core components */

  /* POSTagger */
  "de.tudarmstadt.ukp.dkpro.core" % "de.tudarmstadt.ukp.dkpro.core.opennlp-asl" % "1.10.0",
  /* Segmenter */
  "de.tudarmstadt.ukp.dkpro.core" % "de.tudarmstadt.ukp.dkpro.core.api.segmentation-asl" % "1.10.0",
  "de.tudarmstadt.ukp.dkpro.core" % "de.tudarmstadt.ukp.dkpro.core.clearnlp-asl" % "1.10.0",
  /* StopwordRemover */
  "de.tudarmstadt.ukp.dkpro.core" % "de.tudarmstadt.ukp.dkpro.core.stopwordremover-asl" % "1.10.0",
  /* Lemmatizer */
  "de.tudarmstadt.ukp.dkpro.core" % "de.tudarmstadt.ukp.dkpro.core.ixa-asl" % "1.10.0",
  /* TokenTrimmer */
  "de.tudarmstadt.ukp.dkpro.core" % "de.tudarmstadt.ukp.dkpro.core.tokit-asl" % "1.10.0",
  /* NamedEntityRecognizer */
  "de.tudarmstadt.ukp.dkpro.core" % "de.tudarmstadt.ukp.dkpro.core.corenlp-gpl" % "1.10.0",

  /* UIMA */
  "org.apache.uima" % "uimaj-as-activemq" % "2.10.3",
  "org.apache.uima" % "uimaj-tools" % "2.8.1",

  /* UIMA Logging */
  "org.slf4j" % "slf4j-simple" % "1.6.4",

  /* JSON parsing */
  "io.spray" %% "spray-json" % "1.3.5",

  /* mongodb Driver */
  "org.mongodb.scala" %% "mongo-scala-driver" % "2.7.0",

  /* ConfigFactory for configuration file */
  "com.typesafe" % "config" % "1.4.0",

  /* spark */
  "org.apache.spark" %% "spark-core" % "2.4.4",
  "org.apache.spark" %% "spark-sql" % "2.4.4"
)

/* needed for spark */
excludeDependencies ++= Seq(ExclusionRule("org.apache.hadoop", "hadoop-core"))
dependencyOverrides += "com.google.guava" % "guava" % "15.0"








