package controllers

import javax.inject.{Inject, Singleton}
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}

import scala.concurrent.ExecutionContext

@Singleton
class PreventController @Inject()(cc: ControllerComponents)
                                 (implicit exec: ExecutionContext) extends AbstractController(cc) {




  def preventPage(page:String): Action[AnyContent] = Action { implicit request =>
    Ok(
      page match{
        case "primaryPage" => views.html.cn.prevent.primaryPage()
        case "secondaryPage" => views.html.cn.prevent.secondaryPage()
        case "tertiaryPage" => views.html.cn.prevent.tertiaryPage()
      }
    )
  }

  def preventPageEn(page:String): Action[AnyContent] = Action { implicit request =>
    Ok(
      page match{
        case "primaryPage" => views.html.en.prevent.primaryPage()
        case "secondaryPage" => views.html.en.prevent.secondaryPage()
        case "tertiaryPage" => views.html.en.prevent.tertiaryPage()
      }
    )
  }

}
