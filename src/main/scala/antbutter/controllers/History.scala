package antbutter.controllers

import com.twitter.finatra.Controller
import antbutter.Provider

class History extends Controller {
  get("/history") { _ =>
    render.json(Provider.timestamps).toFuture
  }
}
