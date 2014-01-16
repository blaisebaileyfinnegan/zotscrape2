import scala.xml._
import WebSoc._

object DocumentParser {

  implicit def convertAttrOptionToString(attrToOption: (String, Option[Seq[Node]])): String = {
    attrToOption match {
      case (attr, option) => {
        val seq = option.getOrElse(throw new Error("Nothing was found for attribute: " + attr))
        if (seq.size != 1) throw new Error("Expected a sequence of size 1! Found: " + seq.toString)
        else seq.toString()
      }
    }
  }

  implicit def convertNodeSeqToString(seq: NodeSeq): String =
    if (seq.size != 1) throw new Error("Expected string sequence of size 1! Found: " + seq.toString)
    else seq.text

  implicit def convertNodeSeqToInt(seq: NodeSeq): Int =
    if (seq.size != 1) throw new Error("Expected int sequence of size 1! Found: " + seq.toString)
    else Integer.valueOf(seq.text)

  def apply(document: xml.Elem) = {
    def getMetadata(document: Elem): Metadata = {
      val generated = ("generated", document.attribute("generated"))
      val author = ("author", document.attribute("author"))

      Metadata(generated, author)
    }

    val metadata = getMetadata(document)


    val topLevelResults = document.child.map((n: Node) => n.label match {
      case "term" => Term(n \ "term_yyyyst", n \ "term_year", n \ "term_name", n \ "term_status_msg")
      case "course_list" => None
      case "restriction_codes" => n.nonEmptyChildren.map((n: Node) => {
        Restriction(n \ "restriction_code", n \ "restriction_def")
      })
      case _ => None
    }).filterNot(_ == None)

    val websoc =
  }
}
