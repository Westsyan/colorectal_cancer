package controllers

import javax.inject._
import play.api.mvc._

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class HomeController @Inject()(cc: ControllerComponents) extends AbstractController(cc) {

  def index = Action{implicit request=>
    Ok(views.html.cn.home.index())
  }

  def indexEn = Action{implicit request=>
    Ok(views.html.en.home.index())
  }


  def homePage = Action{implicit request=>
    Redirect(routes.HomeController.index())
  }


}
