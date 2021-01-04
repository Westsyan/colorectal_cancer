package controllers

import javax.inject.Inject
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}

class TreatmentController @Inject()(cc: ControllerComponents) extends AbstractController(cc) {


  def treatmentPage(page: String): Action[AnyContent] = Action { implicit request =>
    Ok(
      page match {
        case "nonmetastaticPage" => views.html.cn.treatment.nonmetastaticPage()
        case "metastaticPage" => views.html.cn.treatment.metastaticPage()
        case "evaluationPage" => views.html.cn.treatment.evaluationPage()
      }
    )
  }

  def treatmentPageEn(page: String): Action[AnyContent] = Action { implicit request =>
    Ok(
      page match {
        case "nonmetastaticPage" => views.html.en.treatment.nonmetastaticPage()
        case "metastaticPage" => views.html.en.treatment.metastaticPage()
        case "evaluationPage" => views.html.en.treatment.evaluationPage()
      }
    )
  }
}
