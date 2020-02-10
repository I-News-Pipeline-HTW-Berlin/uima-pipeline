name := "DK_Pro"

version := "0.1"

scalaVersion := "2.13.1"

resolvers += "ukp-oss-model-releases" at "http://zoidberg.ukp.informatik.tu-darmstadt.de/artifactory/public-model-releases-local"

libraryDependencies ++= Seq(
  /* DKPro core components */
  /* POS Tagger */
  "de.tudarmstadt.ukp.dkpro.core" % "de.tudarmstadt.ukp.dkpro.core.opennlp-asl" % "1.10.0",
  /* Segmenter */
  "de.tudarmstadt.ukp.dkpro.core" % "de.tudarmstadt.ukp.dkpro.core.clearnlp-asl" % "1.10.0",
  "de.tudarmstadt.ukp.dkpro.core" % "de.tudarmstadt.ukp.dkpro.core.api.segmentation-asl" % "1.10.0",
  /* StopWordRemover */
  "de.tudarmstadt.ukp.dkpro.core" % "de.tudarmstadt.ukp.dkpro.core.stopwordremover-asl" % "1.10.0",
  /* Lemmatizer */
  "de.tudarmstadt.ukp.dkpro.core" % "de.tudarmstadt.ukp.dkpro.core.ixa-asl" % "1.10.0",
  /* TrailingCharacterRemover */
  "de.tudarmstadt.ukp.dkpro.core" % "de.tudarmstadt.ukp.dkpro.core.textnormalizer-asl" % "1.10.0",
  /* TokenTrimmer */
  "de.tudarmstadt.ukp.dkpro.core" % "de.tudarmstadt.ukp.dkpro.core.tokit-asl" % "1.10.0",

  /* UIMA */
  "org.apache.uima" % "uimaj-as-activemq" % "2.10.3",
  "org.apache.uima" % "uimaj-tools" % "2.8.1",

  /* UIMA Logging */
  "org.slf4j" % "slf4j-simple" % "1.6.4",

  /* JSON parsing */
  "io.spray" %% "spray-json" % "1.3.5",

  /* mongoDB driver */
  "org.mongodb.scala" %% "mongo-scala-driver" % "2.7.0",

  /* ConfigFactory for configuration file */
  "com.typesafe" % "config" % "1.4.0"

)









