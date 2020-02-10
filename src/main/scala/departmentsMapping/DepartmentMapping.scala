package departmentsMapping

import java.io.{File, IOException}
import json.JSONParser
import org.apache.spark.rdd.RDD
import scala.io.Source
import uima.App

/**
 * Used to map keywords to departments.
 */
object DepartmentMapping {

  val sc = App.getSparkContext

  /**
   * Takes a List of keywords and maps them to a Set of departments by looking up keywords in a dictionary
   * @param keywords
   * @param dep_keywords_dict
   * @return Set[String]
   */
  def getDepartmentsForArticle(keywords: List[String], dep_keywords_dict: RDD[(String, List[String])]): Set[String] = {
    val reversedDict = swap(dep_keywords_dict)
    keywords.foldLeft(List.empty[String])((list, kw) => {
      if(reversedDict.contains(kw)) reversedDict(kw)::list
      else list
    }).toSet
  }

  /**
   * Turns the keyword dictionary into a simple Map.
   * @param dep_keywords_map
   * @return Map[String, String]
   */
  def swap(dep_keywords_map: RDD[(String, List[String])]) : Map[String, String] = {
    dep_keywords_map.flatMap(entry => entry._2.map(kw => (kw, entry._1))).collect.toMap
  }

  /**
   * Deserializes department dictionary from given filepath.
   * @param filePath
   * @return
   */
  def deserialize(filePath: String):  RDD[(String, List[String])] = {
    try {
      if (new File(filePath).exists) {
        val fileSource = Source.fromFile(filePath)
        var jsonString = ""
        val linesIt = fileSource.getLines()
        while (linesIt.hasNext) {
          jsonString += linesIt.next()
        }
        try {
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
      case e: ClassNotFoundException => throw new IOException(e)
    }
  }
}
