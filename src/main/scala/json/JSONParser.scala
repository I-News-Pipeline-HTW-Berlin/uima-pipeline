package json

import spray.json._
import DefaultJsonProtocol._

object JSONParser {

  /**
   * Parses keyword dictionary for department mapping.
   * @param json
   * @return Map[String, List[String]]
   */
  def parseDepartmentKeywordsMapping(json: String) : Map[String, List[String]] = {
    val jsonAst = json.parseJson
    val data = jsonAst.convertTo[Map[String, JsArray]]
    data.map(entry => (entry._1, entry._2 match {
      case JsArray(value) => value.toList.map(jsVal => jsVal.asInstanceOf[JsString].value)
    }))
  }

  /**
   * Parses idf-model.
   * @param json
   * @return List[(String, Double)]
   */
  def parseIdfModel(json: String) : List[(String, Double)] = {
    val jsonAst = json.parseJson
    val data = jsonAst.convertTo[Map[String, JsValue]].toList
    data.map(entry => (entry._1, entry._2 match {
      case JsNumber(value) => value.toDouble
    }))
  }

  /**
   * Parses document text for reader classes.
   * @param json
   * @return
   */
  def parseDocumentText(json: String) : Map[String, String] = {
    val jsonAst = json.parseJson
    val data = jsonAst.convertTo[Map[String, JsValue]]
    data.map(key => (key._1, key._2 match {
      case JsString(value) => value
      case _ => ""
    }))
  }

  /**
   * Parses orignial article.
   * @param json
   * @return
   */
  def parseOriginalArticle(json: String) : Map[String, Any] = {
    val jsonAst = json.parseJson
    val data = jsonAst.convertTo[Map[String, JsValue]]
    data.map(key => (key._1, key._2 match {
      case JsObject(fields) if fields.contains("$date") => fields("$date") match {
        case JsNumber(value) => value
      }
      case JsObject(fields) if fields.contains("$oid") => fields("$oid") match {
        case JsString(value) => value
      }
      case JsArray(elements) => elements.toList.map(jsVal => jsVal.asInstanceOf[JsString].value)
      case JsNull => null
      case JsString(value) => value
    }))
  }
}
