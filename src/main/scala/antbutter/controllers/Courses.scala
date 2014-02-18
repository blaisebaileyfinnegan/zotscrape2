package antbutter.controllers

import com.twitter.finatra.Controller
import antbutter.{PayloadPacker, Provider}

class Courses extends Controller {
  get("/courses") { _ =>
    render.json(Provider.courses map PayloadPacker.course).toFuture
  }
}
