package antbutter

import com.twitter.finatra._

object App extends FinatraServer {
  class Static extends Controller {
    get("/") { request =>
      render.static("index.html").toFuture
    }
  }

  register(new Static())
}
