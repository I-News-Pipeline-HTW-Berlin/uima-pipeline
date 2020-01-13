package json

import de.tudarmstadt.ukp.dkpro.core.api.io.ResourceCollectionReaderBase.Resource
import spray.json._
import DefaultJsonProtocol._
import scala.io.Source

object JSONParser {

  def getJsonStringFromResource(res: Resource) : String = {
    Source.fromInputStream(res.getInputStream).mkString
  }

  def parseDepartmentKeywordsMapping(json: String) : Map[String, List[String]] = {
    val jsonAst = json.parseJson
    val data = jsonAst.convertTo[Map[String, JsArray]]
    data.map(entry => (entry._1, entry._2 match {
      case JsArray(value) => value.toList.map(jsVal => jsVal.asInstanceOf[JsString].value)
    }))
  }

  def parseIdfModel(json: String) : Map[String, Double] = {
    val jsonAst = json.parseJson
    val data = jsonAst.convertTo[Map[String, JsValue]]
    data.map(entry => (entry._1, entry._2 match {
      case JsNumber(value) => value.toDouble
    }))
  }

  def parseStrings(json: String) : Map[String, String] = {

    val jsonAst = json.parseJson
    val data = jsonAst.convertTo[Map[String, JsValue]]
    data.map(key => (key._1, key._2 match {
      /* auto-generated from intellij, vielleicht später hilfreich */
      //case JsObject(fields) =>
    /*  case JsArray(elements) => elements.toList.map(el => el.toString())
      case JsNumber(value) => value
      case boolean: JsBoolean =>
      case JsNull =>*/
      case JsString(value) => value
      case _ => ""
    }))
  }

  def parseAll(json: String) : Map[String, Any] = {
    val jsonAst = json.parseJson
    val data = jsonAst.convertTo[Map[String, JsValue]]
    data.map(key => (key._1, key._2 match {
      /* auto-generated from intellij, vielleicht später hilfreich */
      case JsObject(fields) if fields.contains("$date") => fields("$date") match {
        case JsNumber(value) => value
      }
      case JsObject(fields) if fields.contains("$oid") => fields("$oid") match {
        case JsString(value) => value
      }
      case JsArray(elements) => elements.toList.map(jsVal => jsVal.asInstanceOf[JsString].value)
      //case JsNumber(value) => value
      //case boolean: JsBoolean =>
      case JsNull => null
      case JsString(value) => value
      //case _ => ""
    }))
  }
}
