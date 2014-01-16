import CollectorService.StartScraper
import akka.actor.{Props, ActorSystem}
import com.typesafe.config.ConfigFactory
import org.jsoup.Jsoup
import org.jsoup.nodes.{Element, Document}
import scala.collection.JavaConversions._
import scalaj.http.Http

object ZotScrape extends App {
  val config = ConfigFactory.load()
  val system = ActorSystem("zotscrape-system")

  val baseUrl = config.getString("baseUrl")
  val potentialQuarters = config.getStringList("targetQuarters").toList
  val debug = config.getBoolean("debug")

  val chooseRecentQuarter = potentialQuarters.isEmpty

  val document = Jsoup.parse(Http(baseUrl).asString)

  def getDropdownValues(document: Document, predicate: (Element) => Boolean): List[String] = {
    val selects = document.getElementsByTag("select")
      .toList
      .filter(predicate)

    if (selects.size != 1) throw new Error("Unexpected amount of term dropdowns! Found " + selects.size)
    else selects(0).children().toList.map(_.attr("value")).map(_.trim)
  }

  val quarters = getDropdownValues(document, _.attr("name") == "YearTerm")
  val departments = getDropdownValues(document, _.attr("name") == "Dept").filterNot(_ == "ALL")

  val targetQuarters =
    if (chooseRecentQuarter) List(quarters.head)
    else potentialQuarters

  val collectorService = system.actorOf(Props(classOf[CollectorService], targetQuarters, departments, baseUrl, debug), "CollectorService")
  collectorService ! StartScraper
}