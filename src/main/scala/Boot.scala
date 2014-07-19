import org.eclipse.jetty.server.Server
import org.eclipse.jetty.servlet.{ServletHolder, DefaultServlet}
import org.eclipse.jetty.webapp.WebAppContext
import org.scalatra.servlet.ScalatraListener
import zotscrape.ConfigProvider

object Boot extends App {
  val port = if (System.getenv("PORT") != null) System.getenv("PORT").toInt else 8080

  val server = new Server(port)
  val context = new WebAppContext()
  context setContextPath "/"
  context setResourceBase "src/main/webapp"
  context.addEventListener(new ScalatraListener)

  val defaultServlet = new ServletHolder(new DefaultServlet)
  defaultServlet.setInitParameter("aliases", "true")

  context.addServlet(defaultServlet, "/")

  server.setHandler(context)

  server.start()
  server.join()
}
