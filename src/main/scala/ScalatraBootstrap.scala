import javax.servlet.ServletContext

import org.scalatra.LifeCycle
import web.ZotServlet
import zotscrape.ConfigProvider

class ScalatraBootstrap extends LifeCycle {
  override def init(context: ServletContext) = {
    context.mount(new ZotServlet, "/api/*")
  }
}

