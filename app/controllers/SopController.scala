package controllers

import javax.inject.{Inject, Singleton}
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}

@Singleton
class SopController @Inject()(cc: ControllerComponents) extends AbstractController(cc) {


  def sopPage(page:String): Action[AnyContent] = Action{ implicit request=>
    Ok(
      page match{
        case "stoolPage" => views.html.cn.sop.stoolPage()
        case "metaPage" => views.html.cn.sop.metaPage()
        case "virusPage" => views.html.cn.sop.virusPage()
        case "bfPage" => views.html.cn.sop.bfPage()
        case "phonePage" => views.html.cn.sop.phonePage()
      }
    )
  }

  def sopPageEn(page:String): Action[AnyContent] = Action{ implicit request=>
    Ok(
      page match{
        case "stoolPage" => views.html.en.sop.stoolPage()
        case "metaPage" => views.html.en.sop.metaPage()
        case "virusPage" => views.html.en.sop.virusPage()
        case "bfPage" => views.html.en.sop.bfPage()
        case "phonePage" => views.html.en.sop.phonePage()
      }
    )
  }

}
