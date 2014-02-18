package antbutter.controllers

import com.twitter.finatra.Controller
import antbutter.{PayloadPacker, Provider}

class Terms extends Controller {
  get("/terms") { _ =>
    render.json(Provider.terms map PayloadPacker.term).toFuture
  }
}
