package departmentsMapping

import java.io.{File, IOException}
import json.JSONParser
import scala.io.Source

/**
 * Maps keywords to departments
 */

object DepartmentMapping {


  /**
   * Takes list of keywords and maps them to a list of departments by looking up keywords in a dictionary
   * @param keywords
   * @param dep_keywords_dict
   * @return
   */
  def getDepartmentsForArticle(keywords: List[String], dep_keywords_dict: Map[String, List[String]]): List[String] = {
    dep_keywords_dict.flatMap(dict => keywords.foldLeft(List[String]())((list, entry) =>
      (list, entry) match {
        case a if dict._2.contains(entry) && !list.contains(dict._1) => dict._1::list
        case _ => list
      })).toList
  }

  /**
   * Deserializes department dictionary from file
   * @param
   * @return
   */
  @SuppressWarnings(Array("unchecked"))
  @throws[IOException]
  def deserialize(filePath: String):  Map[String, List[String]] = try {
    try {
      if (new File(filePath).exists) {
        val fileSource = Source.fromFile(filePath)
        var jsonString = ""
        val linesIt = fileSource.getLines()
        while (linesIt.hasNext) {
          jsonString += linesIt.next()
        }
        try {
          return JSONParser.parseDepartmentKeywordsMapping(jsonString)
        }
        Map.empty[String, List[String]]
      }
      else {
        println("departments.json file doesnt exist.")
        Map.empty[String, List[String]]
      }
    }
    catch {
      case e: ClassNotFoundException => throw new IOException(e)
    }
  }


}
