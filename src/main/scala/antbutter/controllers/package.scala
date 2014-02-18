package antbutter

import com.twitter.finatra.Request
import scala.util.Try

package object controllers {
  def extractIntParam(request: Request, param: String): Option[Try[Int]] =
    request.routeParams.get(param) map {
      id => Try(Integer.parseInt(id))
    }

  def extractLongParam(request: Request, param: String): Option[Try[Long]] =
    request.routeParams.get(param) map {
      id => Try(java.lang.Long.parseLong(id))
    }
}
