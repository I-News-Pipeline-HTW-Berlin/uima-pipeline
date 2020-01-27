package departmentsMapping

import java.io.{File, IOException}

import json.JSONParser
import org.apache.spark.rdd.RDD

import scala.io.Source
import uima.App

object DepartmentMapping {

  val sc = App.getSparkContext

  def getDepartmentsForArticle(keywords: List[String], dep_keywords_dict: RDD[(String, List[String])]) = {
    val reversedDict = swap(dep_keywords_dict)
    keywords.foldLeft(List.empty[String])((list, kw) => {
      if(reversedDict.contains(kw)) reversedDict(kw)::list
      else list
    }).toSet
    /*dep_keywords_dict.flatMap(dict => keywords.foldLeft(List[String]())((list, entry) =>
      (list, entry) match {
        case a if dict._2.contains(entry) && !list.contains(dict._1) => dict._1::list
        case _ => list
      })).toList*/
  }

  def swap(dep_keywords_map: RDD[(String, List[String])]) : Map[String, String] = {
    dep_keywords_map.flatMap(entry => entry._2.map(kw => (kw, entry._1))).collect.toMap
  }

 /* def RDDFromFile(path: String, isAResource: Boolean = true): RDD[String] = {
    //val acref = getPath(path, isAResource)
    if(!new File(path).exists()){
      println("departments json file doesnt exist")
      return sc.emptyRDD[String]
    }
    sc.textFile(path)
  }*/

  //@SuppressWarnings(Array("unchecked"))
  //@throws[IOException]
  def deserialize(filePath: String):  RDD[(String, List[String])] = {

    /*val stringRDD = RDDFromFile(filePath)
    val jsonString = stringRDD.fold("")(_+_)
    println(jsonString)
    val parsedJson = JSONParser.parseDepartmentKeywordsMapping(jsonString)
    sc.parallelize(parsedJson.toSeq)*/

    try {
      if (new File(filePath).exists) {
        val fileSource = Source.fromFile(filePath)
        var jsonString = ""
        val linesIt = fileSource.getLines()
        while (linesIt.hasNext) {
          jsonString += linesIt.next()
        }
        try {
          //println("jsonString: "+ jsonString)
          return sc.parallelize(JSONParser.parseDepartmentKeywordsMapping(jsonString).toSeq)
        }
        sc.emptyRDD[(String, List[String])]
      }
      else {
        println("departments.json file doesnt exist.")
        sc.emptyRDD[(String, List[String])]
      }
    }
    catch {
      // warum classnotfoundexception?
      case e: ClassNotFoundException => throw new IOException(e)
    }
  }


}
