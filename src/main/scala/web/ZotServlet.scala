package web

import java.util.concurrent.Executors

import org.scalatra.{AsyncResult, FutureSupport, ScalatraServlet}
import org.json4s.{DefaultFormats, Formats}
import org.scalatra.json._
import zotscrape.ConfigProvider
import zotscrape.writer.Schema

import scala.concurrent.{ExecutionContext, Future}
import scala.slick.driver.MySQLDriver
import scala.slick.driver.MySQLDriver.simple._

class ZotServlet extends ScalatraServlet with JacksonJsonSupport with FutureSupport with ConfigProvider {
  override val configService = new Config(None)
  implicit val s =
    MySQLDriver.simple.Database.forURL(
      configService.Jdbc.url,
      configService.Jdbc.username,
      configService.Jdbc.password)

  protected implicit val jsonFormats: Formats = DefaultFormats
  protected implicit def executor: ExecutionContext =
    ExecutionContext.fromExecutor(Executors.newSingleThreadExecutor)

  before() {
    contentType = formats("json")
  }

  get("/restrictions/") {
    new AsyncResult { val is =
      Future {
        s withSession { implicit session =>
          Schema.restrictions.run(session)
        }
      }
    }
  }

  get("/history/") {
    new AsyncResult { val is =
      Future {
        s withSession { implicit session =>
          Schema.history.run(session).map(_.getTime)
        }
      }
    }
  }

  get("/terms/") {
    new AsyncResult { val is =
      Future {
        s withSession { implicit session =>
          Schema.terms.run(session)
        }
      }
    }
  }

  get("/schools/") {
    new AsyncResult { val is =
      Future {
        s withSession { implicit session =>
          Schema.schools.run(session)
        }
      }
    }
  }

  get("/departments/:school/") {
    val schoolId = params("school").toInt
    new AsyncResult { val is =
      Future {
        s withSession { implicit session =>
          Schema.departments.filter(_.schoolId === schoolId).run(session)
        }
      }
    }
  }

  get("/courses/:department/") {
    val departmentId = params("department").toInt
    new AsyncResult { val is =
      Future {
        s withSession { implicit session =>
          Schema.courses.filter(_.departmentId === departmentId).run(session)
        }
      }
    }
  }

  get("/sections/:course/") {
    val courseId = params("course").toInt
    new AsyncResult { val is =
      Future {
        s withSession { implicit session =>
          Schema.sections.filter(_.courseId === courseId).run(session)
        }
      }
    }
  }
}
