import akka.actor._
import scala.slick.driver.MySQLDriver
import scala.slick.driver.MySQLDriver.simple._
import scala.slick.jdbc.meta.MTable
import scala.slick.lifted.ProvenShape
import com.mysql.jdbc.Driver

object WriterService {
  case class CreateSchema(session: MySQLDriver.backend.Session)
}

object Schema {
  class Coffees(tag: Tag) extends Table[(String)](tag, "coffees") {
    def name = column[String]("name", O.PrimaryKey)

    def * = name
  }
}

object DatabaseObjects {
  import Schema._

  val coffees = TableQuery[Coffees]
}

class WriterService extends Actor with ActorLogging {
  import Manager._
  import WriterService._
  import DatabaseObjects._

  def receive = {
    case CreateSchema(session) => session asDynamicSession {
      implicit val _ = session

      log.info("Creating schema.")
      coffees.ddl.create
    }

    case StartWriterService => {
      implicit val session = Database.forURL("jdbc:mysql://localhost/zotscrape2", "root").createSession()
      session asDynamicSession {
        if (MTable.getTables("coffees").list().isEmpty)
          self ! CreateSchema(session)
      }
    }
    case unrecognized => log.error("Unexpected message " + unrecognized + " sent to writer service.")
  }
}
