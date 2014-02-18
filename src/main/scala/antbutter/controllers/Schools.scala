package antbutter.controllers

import com.twitter.finatra.Controller
import antbutter.{PayloadPacker, Provider}

class Schools extends Controller {
  get("/schools") { _ =>
    render.json(Provider.schools map PayloadPacker.school).toFuture
  }
}
