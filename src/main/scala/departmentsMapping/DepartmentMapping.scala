package departmentsMapping

import java.io.{File, FileInputStream, IOException, ObjectInputStream}

import json.JSONParser
import spray.json.JsValue

import scala.io.Source

object DepartmentMapping {


  def getDepartmentsForArticle(keywords: List[String], dep_keywords_dict: Map[String, List[String]]) = {
    dep_keywords_dict.flatMap(dict => keywords.foldLeft(List[String]())((list, entry) =>
      (list, entry) match {
        case a if dict._2.contains(entry) && !list.contains(dict._1) => dict._1::list
        case _ => list
      })).toList
  }

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
          println(jsonString)
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
      // warum classnotfoundexception?
      case e: ClassNotFoundException => throw new IOException(e)
    }
  }


}
