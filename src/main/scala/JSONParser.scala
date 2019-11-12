import de.tudarmstadt.ukp.dkpro.core.api.io.ResourceCollectionReaderBase.Resource
import spray.json._
import DefaultJsonProtocol._

import scala.io.Source

object JSONParser {
  def parse(res: Resource) : Map[String, String] = {
    val json = Source.fromInputStream(res.getInputStream).mkString

    val jsonAst = json.parseJson
    val data = jsonAst.convertTo[Map[String, JsValue]]

    data.map(key => (key._1, key._2 match {
      /* auto-generated from intellij, vielleicht später hilfreich */
      /*case JsObject(fields) =>
      case JsArray(elements) =>
      case JsString(value) =>
      case JsNumber(value) =>
      case boolean: JsBoolean =>
      case JsNull =>*/
      case JsString(value) => value
      case _ => ""
    }))
  }
}
