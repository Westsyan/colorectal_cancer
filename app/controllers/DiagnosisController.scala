package controllers

import javax.inject.Inject
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}

class DiagnosisController@Inject()(cc: ControllerComponents) extends AbstractController(cc) {

  def diagnosisPage(page:String): Action[AnyContent] = Action{ implicit request=>
    Ok(
      page match{
        case "diseasePage" => views.html.cn.diagnosis.diseasePage()
        case "drugPage" => views.html.cn.diagnosis.drugPage()
        case "prognosisPage" => views.html.cn.diagnosis.prognosisPage()
        case "biologicalPage" => views.html.cn.diagnosis.biologicalPage()
      }
    )
  }

  def diagnosisPageEn(page:String): Action[AnyContent] = Action{ implicit request=>
    Ok(
      page match{
        case "diseasePage" => views.html.en.diagnosis.diseasePage()
        case "drugPage" => views.html.en.diagnosis.drugPage()
        case "prognosisPage" => views.html.en.diagnosis.prognosisPage()
        case "biologicalPage" => views.html.en.diagnosis.biologicalPage()
      }
    )
  }
}
