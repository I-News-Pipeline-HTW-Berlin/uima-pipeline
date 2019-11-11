name := "DK_Pro"

version := "0.1"

scalaVersion := "2.13.1"

libraryDependencies ++= Seq(
  /* DKPro core components */
  "de.tudarmstadt.ukp.dkpro.core" % "de.tudarmstadt.ukp.dkpro.core.clearnlp-asl" % "1.10.0",
  "de.tudarmstadt.ukp.dkpro.core" % "de.tudarmstadt.ukp.dkpro.core.opennlp-asl" % "1.10.0",
  "de.tudarmstadt.ukp.dkpro.core" % "de.tudarmstadt.ukp.dkpro.core.io.text-asl" % "1.10.0",
  "de.tudarmstadt.ukp.dkpro.core" % "de.tudarmstadt.ukp.dkpro.core.api.segmentation-asl" % "1.10.0",
  "de.tudarmstadt.ukp.dkpro.core" % "de.tudarmstadt.ukp.dkpro.core.stopwordremover-asl" % "1.10.0",
  "de.tudarmstadt.ukp.dkpro.core" % "de.tudarmstadt.ukp.dkpro.core.languagetool-asl" % "1.10.0",

  /* UIMA */
  "org.apache.uima" % "uimaj-as-activemq" % "2.10.3",

  /* UIMA Logging */
  "org.slf4j" % "slf4j-simple" % "1.6.4",

  /* JSON parsing */
  "io.spray" %% "spray-json" % "1.3.5"
)