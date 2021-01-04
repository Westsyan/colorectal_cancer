package controllers

import config.{MyConfig, MyRow}
import dao.SampleDao
import javax.inject.{Inject, Singleton}
import models.Tables.SampleRow
import play.api.libs.json.Json
import play.api.mvc.{AbstractController, ControllerComponents}
import utils.ReadExcel

import scala.collection.mutable
import scala.concurrent.ExecutionContext


@Singleton
class SampleController@Inject()(cc:ControllerComponents,sampleDao:SampleDao)
                               (implicit execution:ExecutionContext) extends AbstractController(cc)
  with MyConfig with MyRow{

  def insert = Action{
    val lines =  ReadExcel.xlsx2Lines("D:\\结直肠癌诊疗系统/sample.xlsx".toFile)
    val w = lines.map(_.split("\t").toList).toList
    val head = w.head
    val row =  w.tail.map{x=>
      val w : mutable.LinkedHashMap[String,String] = mutable.LinkedHashMap()
      val r = x.zipWithIndex.drop(4).map{y=>
        w.put(head(y._2),y._1)
      }
     SampleRow(0, Json.toJson(w).toString())
    }
    sampleDao.insert(row).toAwait
    Ok(Json.toJson("success"))
  }

  def samplePage = Action{implicit request=>
    Ok(views.html.cn.samples.samplesPage())
  }

  def samplePageEn = Action{implicit request=>
    Ok(views.html.en.samples.samplesPage())
  }

  def getAllSample = Action{
    val rows = sampleDao.getAllSample.toAwait
    val q = rows.map{x=>
      Json.parse(x.samples)
    }

    Ok(Json.toJson(q))
  }

  def sampleTyping = Action{implicit request=>
    Ok(views.html.cn.samples.sampleTyping())
  }

  def sampleTypingEn = Action{implicit request=>
    Ok(views.html.en.samples.sampleTyping())
  }

  def getData =Action{implicit request=>
    val w = request.body.asFormUrlEncoded
    val json = Json.toJson(w.get.map(x=>x._1->x._2.mkString))

    sampleDao.insert(Seq(SampleRow(0,json.toString()))).toAwait

    Ok(Json.toJson("success"))
  }


}
