package controllers

import config.MyConfig
import dao.CBioPortalDao
import javax.inject.Inject
import models.Tables.CbioportalfilesRow
import play.api.libs.json.Json
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}
import utils.{Global, TableUtils}
import utils.TableUtils.pageForm

import scala.concurrent.ExecutionContext
import scala.io.Source

class PublicdbController @Inject()(cc: ControllerComponents, cBioPortalDao: CBioPortalDao)
                                  (implicit exec: ExecutionContext) extends AbstractController(cc) with MyConfig {


  def indexPage = Action { implicit request =>
    Ok(views.html.cn.public.indexPage())
  }

  def indexPageEn = Action { implicit request =>
    Ok(views.html.en.public.indexPage())
  }

  def seerPage = Action { implicit request =>
    Ok(views.html.cn.public.seersPage())
  }

  def seerPageEn = Action { implicit request =>
    Ok(views.html.en.public.seersPage())
  }

  def getAllSeerData: Action[AnyContent] = Action { implicit request =>
    val page = pageForm.bindFromRequest.get
    val orderX = TableUtils.dealMapDataByPage(TableUtils.seerRow, page)
    val total = orderX.size
    val tmpX = orderX.slice(page.offset, page.offset + page.limit)
    val json = tmpX
    Ok(Json.obj("rows" -> json, "total" -> total))
  }

  def cBioPortalPage = Action { implicit request =>
    Ok(views.html.cn.public.cbioportalPage())
  }

  def cBioPortalPageEn = Action { implicit request =>
    Ok(views.html.en.public.cbioportalPage())
  }

  def cbioportalFilesPage(name: String) = Action { implicit request =>
    Ok(views.html.cn.public.cbioportalFilesPage(name))
  }

  def cbioportalFilesPageEn(name: String) = Action { implicit request =>
    Ok(views.html.en.public.cbioportalFilesPage(name))
  }

  def cbioportalDbPage(db: String, name: String) = Action { implicit request =>
    Ok(views.html.cn.public.cbioportalDbPage(db, name))
  }

  def cbioportalDbPageEn(db: String, name: String) = Action { implicit request =>
    Ok(views.html.en.public.cbioportalDbPage(db, name))
  }

  def getCBioPortalFileList(name: String) = Action.async { implicit request =>
    val fileName = Global.cbioportalMap(name)
    cBioPortalDao.getByName(fileName).map { x =>
      val row = x.map { y =>
        Json.obj("data" -> y.f2, "meta" -> y.meta)
      }
      Ok(Json.toJson(row))
    }
  }


  def getCBioPortalDataHeader(db: String, name: String) = Action { implicit request =>
    val file = s"${Global.path}/public/cBioPortal/${Global.cbioportalMap(db)}/data_$name.txt".toFile
    val files = Source.fromFile(file)
    val lines = files.getLines()
    val header = lines.slice(0, 1).toSeq.head.split("\t")
    val total = lines.length
    Ok(Json.obj("header" -> header, "total" -> total))
  }

  def getCBioPortalData(db: String, name: String, total: Int) = Action { implicit request =>
    val page = pageForm.bindFromRequest.get
    val dbs = Global.cbioportalMap(db)
    val file = s"${Global.path}/public/cBioPortal/$dbs/data_$name.txt".toFile
    val files = Source.fromFile(file)
    val lines = files.getLines()
    val header = lines.slice(0, 1).toSeq.head.split("\t")
    val tmpX = lines.slice(page.offset, page.offset + page.limit).toSeq.map(_.split("\t"))
    val json = tmpX.map { x =>
      x.zipWithIndex.map { y =>
        header(y._2) -> y._1
      }.toMap
    }
    Ok(Json.obj("rows" -> json, "total" -> total))

  }

  def insertCbio = Action {
    val rows = s"${Global.path}/public/cBioPortal".toFile.listFiles().flatMap { x =>
      val f1 = x.getName
      x.listFiles.groupBy(_.getName.split("_").tail.mkString("_")).map { z =>
        val meta = z._2.find(_.getName.startsWith("meta")).get.readLines.mkString("<br/>")
        println(f1, z._1, meta)
        CbioportalfilesRow(0, f1, z._1.dropRight(4), meta)
      }.toSeq
    }
    cBioPortalDao.insert(rows).toAwait
    Ok(Json.toJson("success!"))
  }

  def tcgaPage = Action { implicit request =>
    Ok(views.html.cn.public.tcgaPage())
  }

  def tcgaPageEn = Action { implicit request =>
    Ok(views.html.en.public.tcgaPage())
  }

  def getTcgaData = Action { implicit request =>
    val page = pageForm.bindFromRequest.get
    val orderX = TableUtils.dealMapDataByPage(TableUtils.tcgaRow, page)
    val total = orderX.size
    val tmpX = orderX.slice(page.offset, page.offset + page.limit)
    val json = tmpX
    Ok(Json.obj("rows" -> json, "total" -> total))
  }


  def oncominePage = Action { implicit request =>
    Ok(views.html.cn.public.oncominePage())
  }

  def oncominePageEn = Action { implicit request =>
    Ok(views.html.en.public.oncominePage())
  }

  def ncbiPage = Action { implicit request =>
    Ok(views.html.cn.public.ncbiPage())
  }

  def ncbiPageEn = Action { implicit request =>
    Ok(views.html.en.public.ncbiPage())
  }

  def getNcbiData = Action { implicit request =>
    Ok(Json.toJson(TableUtils.ncbiRow))
  }

}
