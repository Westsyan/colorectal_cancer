package controllers

import java.io.File
import java.nio.file.Files

import config.MyConfig
import javax.inject.{Inject, Singleton}
import play.api.mvc.{AbstractController, ControllerComponents, Headers}
import utils.Global

import scala.concurrent.ExecutionContext

@Singleton
class UtilsController @Inject()(cc: ControllerComponents)(implicit exec: ExecutionContext)
  extends AbstractController(cc) with MyConfig {

  def getImage(path: String) = Action { implicit request =>
    val file =  s"${Global.path}/$path".toFile
    SendImg(file, request.headers)
  }

  def getToolsImage(path:String,num:String)= Action { implicit request =>
    val file =  s"${Global.path}/data/${request.userId}/tools/$path".toFile
    SendImg(file, request.headers)
  }


  def SendImg(file: File, headers: Headers) = {
    val lastModifiedStr = file.lastModified().toString
    val MimeType = "image/jpg"
    val byteArray = Files.readAllBytes(file.toPath)
    val ifModifiedSinceStr = headers.get(IF_MODIFIED_SINCE)
    if (ifModifiedSinceStr.isDefined && ifModifiedSinceStr.get == lastModifiedStr) {
      NotModified
    } else {
      Ok(byteArray).as(MimeType).withHeaders(
        LAST_MODIFIED -> file.lastModified().toString
      )
    }
  }

  def downloadExample(file:String) = Action{implicit request=>
    Ok.sendFile(s"${Global.path}/example/$file".toFile).withHeaders(
      //缓存
      CACHE_CONTROL -> "max-age=3600",
      CONTENT_DISPOSITION -> s"attachment; filename=$file",
      CONTENT_TYPE -> "application/x-download"
    )
  }

  def downloadToolsFile(path:String) = Action{implicit request=>
    val name = path.split("/").last
    Ok.sendFile(s"${Global.path}/data/${request.userId}/tools/$path".toFile).withHeaders(
    //缓存
    CACHE_CONTROL -> "max-age=3600",
    CONTENT_DISPOSITION -> s"attachment; filename=$name",
    CONTENT_TYPE -> "application/x-download"
  )


  }


}
