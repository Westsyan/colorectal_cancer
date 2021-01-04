package filters

import akka.stream.Materializer
import controllers.routes
import javax.inject._
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}

/**
 * This is a simple filter that adds a header to all requests. It's
 * added to the application's list of filters by the
 * [[Filters]] class.
 *
 * @param mat This object is needed to handle streaming of requests
 * and responses.
 * @param exec This class is needed to execute code asynchronously.
 * It is used below by the `map` method.
 */
@Singleton
class ExampleFilter @Inject()(
    implicit override val mat: Materializer,
    exec: ExecutionContext) extends Filter {

  override def apply(f: (RequestHeader) => Future[Result])(rh: RequestHeader): Future[Result] = {
    if (rh.session.get("id").isEmpty && rh.path != "/" && rh.path.contains("platform")  && !rh.path.contains("/assets/") ) {
      Future.successful(Results.Redirect(routes.UserController.loginPage()))
    } else {
      f(rh)
    }
  }

}
