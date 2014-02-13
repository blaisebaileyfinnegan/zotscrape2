package antbutter

import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers
import com.twitter.finatra.test._
import com.twitter.finatra.FinatraServer
import antbutter._

class AppSpec extends FlatSpecHelper {
  val app = new App.Static
  override val server = new FinatraServer
  server.register(app)

  "GET /" should "respond with index.html" in {
    get("/", Map.empty, Map.empty)
    response.code should be (200)
  }
}
