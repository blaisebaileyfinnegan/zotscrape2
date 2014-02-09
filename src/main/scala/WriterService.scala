import akka.actor._
import scala.slick.driver.MySQLDriver
import scala.slick.driver.MySQLDriver.simple._
import scala.slick.jdbc.meta.MTable

object WriterService {
  case class CreateSchema(session: MySQLDriver.backend.Session)
}

object DatabaseObjects {
  import Schema._
}

class WriterService extends Actor with ActorLogging {
  import Manager._
  import WriterService._
  import DatabaseObjects._

  def receive = {
    case CreateSchema(session) => session asDynamicSession {
      implicit val _ = session
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
