package antbutter.controllers

import com.twitter.finatra.Controller
import antbutter.{PayloadPacker, Provider}


class Departments extends Controller {
  get("/departments") { _ =>
    render.json(Provider.departments map PayloadPacker.department).toFuture
  }
}

