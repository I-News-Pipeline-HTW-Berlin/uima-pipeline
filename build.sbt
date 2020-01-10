name := "DK_Pro"

version := "0.1"

scalaVersion := "2.13.1"

//scalaVersion := "2.12"

libraryDependencies ++= Seq(
  /* DKPro core components */
  "de.tudarmstadt.ukp.dkpro.core" % "de.tudarmstadt.ukp.dkpro.core.clearnlp-asl" % "1.10.0",
  "de.tudarmstadt.ukp.dkpro.core" % "de.tudarmstadt.ukp.dkpro.core.opennlp-asl" % "1.10.0",
  "de.tudarmstadt.ukp.dkpro.core" % "de.tudarmstadt.ukp.dkpro.core.io.text-asl" % "1.10.0",
  "de.tudarmstadt.ukp.dkpro.core" % "de.tudarmstadt.ukp.dkpro.core.api.segmentation-asl" % "1.10.0",
  "de.tudarmstadt.ukp.dkpro.core" % "de.tudarmstadt.ukp.dkpro.core.stopwordremover-asl" % "1.10.0",
  /*"de.tudarmstadt.ukp.dkpro.core" % "de.tudarmstadt.ukp.dkpro.core.languagetool-asl" % "1.10.0",*/
  "de.tudarmstadt.ukp.dkpro.core" % "de.tudarmstadt.ukp.dkpro.core.ixa-asl" % "1.10.0",
  "de.tudarmstadt.ukp.dkpro.core" % "de.tudarmstadt.ukp.dkpro.core.io.json-asl" % "1.10.0",
  "de.tudarmstadt.ukp.dkpro.core" % "de.tudarmstadt.ukp.dkpro.core.frequency-asl" % "1.10.0",
  "de.tudarmstadt.ukp.dkpro.core" % "de.tudarmstadt.ukp.dkpro.core.textnormalizer-asl" % "1.10.0",
  "de.tudarmstadt.ukp.dkpro.core" % "de.tudarmstadt.ukp.dkpro.core.tokit-asl" % "1.10.0",
  "de.tudarmstadt.ukp.dkpro.core" % "de.tudarmstadt.ukp.dkpro.core.stanfordnlp-gpl" % "1.10.0",

  /* UIMA */
  "org.apache.uima" % "uimaj-as-activemq" % "2.10.3",

  /* UIMA Logging */
  "org.slf4j" % "slf4j-simple" % "1.6.4",

  /* JSON parsing */
  "io.spray" %% "spray-json" % "1.3.5",

  "org.apache.uima" % "uimaj-tools" % "2.8.1",

  "org.mongodb.scala" %% "mongo-scala-driver" % "2.7.0"

)

 /* assemblyShadeRules in assembly := Seq(
    case PathList("META-INF/org.apache.uima.fit/fsindexes.txt") =>
      ShadeRule.rename("de.tudarmstadt.ukp.dkpro.core.api.**" -> "newName.@1").inAll
  )*/

  /*assemblyShadeRules in assembly := Seq(
    //ShadeRule.rename("META-INF.org.apache.uima.fit.types.txt" -> "shadeio").inAll
    ShadeRule.rename("de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.**" ->"shadeio.@1").inAll
  )*/

  /*assemblyShadeRules in assembly := Seq(
      ShadeRule.rename("org.apache.uima.fit.**" -> "my_conf.@1")
        .inLibrary("de.tudarmstadt.ukp.dkpro.core" % "de.tudarmstadt.ukp.dkpro.core.api.segmentation-asl" % "1.10.0")
  )*/


//META-INF/org.apache.uima.fit/types.txt\

/*assemblyMergeStrategy in assembly := {
case PathList("META-INF", xs @ _*) => MergeStrategy.discard
case x => MergeStrategy.first
}*/

  /*assemblyMergeStrategy in assembly := {
    case PathList("META-INF", xs @ _*) => MergeStrategy.discard
    case x => val oldStrategy = (assemblyMergeStrategy in assembly).value
      oldStrategy(x) }*/







