package antbutter

import java.sql.Timestamp
import spray.json._

trait BigPayloads { this: RepoProvider =>

  def getSectionWithExtras(ccode: Int, termId: Int)(timestamp: Timestamp) =
    repoService.sectionByCcode(ccode, termId)(timestamp).map { section =>
      val sectionId = section._1
      Map[String, JsValue](
        "section" -> JsObject(PayloadPacker.section(section)),
        "meetings" -> JsArray(repoService.meetingsBySectionId(sectionId)
          .map(PayloadPacker.meeting)
          .map(JsObject(_))
          .toList),
        "final" -> repoService.finalBySectionId(sectionId)
          .map(PayloadPacker.fina)
          .map(JsObject(_))
          .getOrElse(JsNull),
        "restrictions" -> JsArray(repoService.restrictionsBySectionId(sectionId)
          .map(PayloadPacker.restriction)
          .map(JsObject(_))
          .toList),
        "instructors" -> JsArray(repoService.instructorsBySectionId(sectionId)
          .map(PayloadPacker.instructor)
          .map(JsObject(_))
          .toList),
        "enrollment" -> repoService.enrollmentBySectionId(sectionId)
          .map(PayloadPacker.enrollment)
          .map(JsObject(_))
          .getOrElse(JsNull)
      )
    }

}
