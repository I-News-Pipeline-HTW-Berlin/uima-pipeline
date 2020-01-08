val keywords_sample: List[String] = List("Bahn AG", "Deutsche Bahn", "Verspätung", "Verkehr",
  "Öko", "Politik", "Zug")

val departments_dict = Map("Politik" -> List(
    "Politik", "Verkehr", "Deutschland", "Europa", "Amerika", "Afrika", "Asien",
  "Nahost", "Netzpolitik"),
                "Umwelt" -> List(
    "Umwelt","Umweltpolitik","Öko","Verkehr","Ökologie","Natur","Klimawandel","Klima",
    "Automobilindustrie","Fahrberichte","Elektromobilität","Fahrrad","Oldtimer",
    "Verkehrsrecht / Service","Führerscheintest"))

//final method:
def getDepartmentsForArticle(keywords: List[String], dep_keywords_dict: Map[String, List[String]]) = {
  dep_keywords_dict.flatMap(dict => keywords.foldLeft(List[String]())((list, entry) =>
    (list, entry) match {
      case a if dict._2.contains(entry) && !list.contains(dict._1) => dict._1::list
      case _ => list
    })).toList
}

getDepartmentsForArticle(keywords_sample, departments_dict)









//Test für Suche von nur "Verkehr"
val str = departments_dict.map(dic => if(dic._2.contains("Verkehr")) dic._1 else "Not in list")

//zwischentest
val testArr = List("erstes", "zweites", "erstes", "drittes")
//testArr.foldLeft(List[String]())((list, wort) => wort::list).distinct //geht!!
testArr.foldLeft(List[String]())((list, wort) => (list,wort) match{
  case a if !list.contains(wort) => wort::list
  case _ => list
})





