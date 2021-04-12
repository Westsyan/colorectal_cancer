package utils

import java.lang.reflect.Field

import config.MyRow
import play.api.data.Form
import play.api.data.Forms._

case class PageData(limit: Int, offset: Int, order: Option[String], search: Option[String], sort: Option[String])

object TableUtils extends MyRow {


  var seerRow:Seq[Map[String,String]] = Seq()

  var tcgaRow:Seq[Map[String,String]] = Seq()

  var searchSeq: Seq[(Int, String)] = Seq()

  var ncbiRow:Seq[Map[String,String]] = Seq()

  def isDouble(value: String): Boolean = {
    try {
      value.toDouble
    } catch {
      case _: Exception =>
        return false
    }
    true
  }

  val pageForm = Form(
    mapping(
      "limit" -> number,
      "offset" -> number,
      "order" -> optional(text),
      "search" -> optional(text),
      "sort" -> optional(text)
    )(PageData.apply)(PageData.unapply)
  )


  def dealMapDataByPage(x: Seq[Map[String, String]], page: PageData): Seq[Map[String, String]] = {
    val searchX = page.search match {
      case None => x
      case Some(y) =>
        val json = Utils.jsonToMap(y).map { q =>
          (q._1, q._2.asInstanceOf[Map[String, String]])
        }

        x.filter { m =>
          json.forall { case (k, v) =>
            v("searchType") match {
              case "num" =>
                val dataMap = v("data").asInstanceOf[Map[String, String]]
                try {
                  val existMin = dataMap("min").toDoubleOption.nonEmpty
                  val existMax = dataMap("max").toDoubleOption.nonEmpty
                  if (existMin && existMax) {
                    m(k).toDouble >= dataMap("min").toDouble && m(k).toDouble <= dataMap("max").toDouble
                  } else if (existMin && !existMax) {
                    m(k).toDouble >= dataMap("min").toDouble
                  } else if (!existMin && existMax) {
                    m(k).toDouble <= dataMap("max").toDouble
                  } else {
                    false
                  }
                } catch {
                  case e: Exception => false
                }
              case "date" =>
                val dataMap = v("data").asInstanceOf[Map[String, String]]
                val existMin = dataMap("min")
                val existMax = dataMap("max")
                if (existMax != "" && existMin != "") {
                  m(k) >= existMin && m(k) < existMax
                } else if (existMax == "" && existMin != "") {
                  m(k) >= existMin
                } else if (existMax != "" && existMin == "") {
                  m(k) < existMax
                } else {
                  true
                }
              case "radio" => m(k) == v("data")
              case "checkbox" =>
                if(v("data") != ""){
                  val dataArray = v("data").asInstanceOf[List[String]]
                  dataArray.indexOf(m(k)) != -1
                }else{
                  true
                }
              case "text" =>
                v("data").split(" ").forall(t => m(k).indexOf(t) != -1)
            }
          }
        }
    }


    val sortX = page.sort match {
      case None => searchX
      case Some(y) =>
        val values = searchX.map(_ (y))
        val b = values.take(1000).forall { value =>
          isDouble(value)
        }
        if (b) {
          searchX.sortBy(_ (y).toDouble)
        } else {
          searchX.sortBy {
            _ (y)
          }
        }
    }
    val orderX = page.order match {
      case None => sortX
      case Some("asc") => sortX
      case Some("desc") => sortX.reverse
    }
    orderX
  }

  def dealDataByPage[T](x: Seq[T], page: PageData): Seq[T] = {
    val searchX = x.filter { y =>
      page.search match {
        case None => true
        case Some(text) =>
          val array = text.split("\\s+").map(_.toUpperCase).toBuffer
          val texts = y.getClass.getDeclaredFields.toBuffer.map { x: Field =>
            x.setAccessible(true)
            x.get(y).toString
          }.init
          array.forall { z =>
            texts.map(_.toUpperCase.indexOf(z) != -1).reduce(_ || _)
          }
      }
    }

    val sortX = page.sort match {
      case None => searchX
      case Some(y) =>
        val b = searchX.take(1000).forall { tmpData =>
          val method = tmpData.getClass.getDeclaredMethod(y)
          val value = method.invoke(tmpData).toString
          isDouble(value)
        }
        if (b) {
          searchX.sortBy { z =>
            val method = z.getClass.getDeclaredMethod(y)
            method.invoke(z).toString.toDouble
          }
        } else {
          searchX.sortBy { z =>
            val method = z.getClass.getDeclaredMethod(y)
            method.invoke(z).toString
          }
        }
    }

    val orderX = page.order match {
      case None => sortX
      case Some("asc") => sortX
      case Some("desc") => sortX.reverse
    }

    orderX

  }


}


