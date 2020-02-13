package json

import spray.json.DefaultJsonProtocol._
import spray.json._

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
   * @return Map[String, Double]
   */
  def parseIdfModel(json: String) : Map[String, Double] = {
    val jsonAst = json.parseJson
    val data = jsonAst.convertTo[Map[String, JsValue]]
    data.map(entry => (entry._1, entry._2 match {
      case JsNumber(value) => value.toDouble
    }))
  }

  /**
   * Parses document text for reader classes.
   * @param json
   * @return Map[String, String]
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
   * Parses original article.
   * @param json
   * @return Map[String, Any]
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
