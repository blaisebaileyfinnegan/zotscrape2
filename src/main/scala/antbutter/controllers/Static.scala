package antbutter.controllers

import com.twitter.finatra.Controller

class Static extends Controller {
  get("/") { _ =>
    render.static("index.html").toFuture
  }
}
