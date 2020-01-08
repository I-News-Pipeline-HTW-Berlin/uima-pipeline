package departmentsMapping

import java.io.{File, FileInputStream, IOException, ObjectInputStream}

import json.JSONParser

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
  def deserialize(filePath: String): Map[String, List[String]] = try {
    if(! new File(filePath).exists()) {
      println("file doesnt exist")
      return Map.empty[String, List[String]]
    }
    val in = new ObjectInputStream(new FileInputStream(new File(filePath)))
    try{
      val jsonString = in.readObject.asInstanceOf[String]
      JSONParser.parseDepartmentKeywordsMapping(jsonString)
    }
    catch {
      // warum classnotfoundexception?
      case e: ClassNotFoundException =>
        throw new IOException(e)
    } finally if (in != null) in.close()
  }
}
