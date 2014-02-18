package antbutter

import com.twitter.finatra._
import scala.util.{Success, Failure, Try}
import java.sql.Timestamp

object App extends FinatraServer {
  import antbutter.controllers._

  lazy val currTimestamp: Timestamp = Provider.maxTimestamp.getOrElse(throw new Error("Can't retrieve timestamp!"))

  register(new Static())
  register(new History())
  register(new Terms())
  register(new Schools())
  register(new Departments())
  register(new Courses())
  register(new Sections())
}
