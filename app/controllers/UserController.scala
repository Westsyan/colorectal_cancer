package controllers

import config.MyAwait
import dao.UserDao
import javax.inject.{Inject, Singleton}
import models.Tables.UserRow
import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.json.Json
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}

@Singleton
class UserController @Inject()(userDao: UserDao, cc: ControllerComponents) extends AbstractController(cc) with MyAwait {


  def loginPage = Action { implicit request =>
    Ok(views.html.cn.user.login()).withNewSession
  }

  def loginPageEn = Action { implicit request =>
    Ok(views.html.en.user.login()).withNewSession
  }

  def registerPage = Action { implicit request =>
    Ok(views.html.cn.user.register()).withNewSession
  }

  def registerPageEn = Action { implicit request =>
    Ok(views.html.en.user.register()).withNewSession
  }

  def registerSuccess = Action { implicit request =>
    Ok(views.html.cn.user.success())
  }

  def registerSuccessEn = Action { implicit request =>
    Ok(views.html.en.user.success())
  }

  case class UserData(user: String, pwd: String)

  val userForm: Form[UserData] = Form(
    mapping(
      "user" -> text,
      "pwd" -> text
    )(UserData.apply)(UserData.unapply)
  )

  def checkLogin: Action[AnyContent] = Action { implicit request =>
    val data = userForm.bindFromRequest.get
    val checkPwd = userDao.checkPwd(data.user, data.pwd).toAwait
    Ok(Json.toJson(checkPwd.toString))
  }

  def loginSuccess: Action[AnyContent] = Action { implicit request =>
    val data = userForm.bindFromRequest.get
    val userId = userDao.getUserId(data.user, data.pwd).toAwait.toString
    Redirect(routes.MlController.modelPage("logistics")).withNewSession.withSession("id" -> userId, "user" -> data.user)
  }

  def register: Action[AnyContent] = Action { implicit request =>
    var valid = "true"
    var message = ""
    try {
      val data = userForm.bindFromRequest.get
      userDao.addUser(UserRow(0, data.user, data.pwd)).toAwait
    } catch {
      case e: Exception =>
        valid = "false"
        message = e.getMessage
    }
    Ok(Json.obj("valid" -> valid, "msg" -> message))
  }

  case class accountData(user: String)

  val accountForm = Form(
    mapping(
      "user" -> text
    )(accountData.apply)(accountData.unapply)
  )

  def checkAccount = Action { implicit request =>
    val data = accountForm.bindFromRequest.get
    val account = data.user
    val valid = userDao.checkUser(account).toAwait
    val json = Json.obj("valid" -> (!valid).toString)
    Ok(Json.toJson(json))
  }


}
