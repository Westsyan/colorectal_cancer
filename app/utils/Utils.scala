package utils

import java.text.SimpleDateFormat
import java.util.Date

import org.joda.time.DateTime
import play.api.libs.json.Json
import play.api.mvc.Request
import shapeless._
import shapeless.ops.hlist.ToList
import shapeless.ops.record._
import shapeless.record._

import scala.util.parsing.json.JSON

object Utils {

  def getValue[T](kind: T, noneMessage: String = ""): String = {
    kind match {
      case x if x.isInstanceOf[DateTime] => val time = x.asInstanceOf[DateTime]
        time.toString("yyyy-MM-dd HH:mm:ss")
      case x if x.isInstanceOf[Option[T]] => val option = x.asInstanceOf[Option[T]]
        if (option.isDefined) getValue(option.get, noneMessage) else noneMessage
      case x if x.isInstanceOf[Seq[T]] => val list = x.asInstanceOf[Seq[T]]
        list.mkString(";")
      case _ => kind.toString
    }
  }

  def getJsonByT[T, R <: HList](y: T)(implicit gen: LabelledGeneric.Aux[T, R],
                                      toMap: ToMap.Aux[R, Symbol, Any]) = {
    val map = gen.to(y).toMap.map { case (symbol, value) =>
      (symbol.name, getValue(value))
    }
    Json.toJson(map)
  }

//  def lines[R <: HList, K <: HList, V <: HList](
//                                                 implicit gen: LabelledGeneric.Aux[T, R], keys: Keys.Aux[R, K],
//                                                 values: Values.Aux[R, V],
//                                                 ktl: ToList[K, Symbol],
//                                                 vtl: ToList[V, Any]
//                                               )(list:List[T]) = {
//    Utils.getLinesByTs(list)
//  }

  def getLinesByTs[T, R <: HList, K <: HList, V <: HList](ys: List[T])(
    implicit gen: LabelledGeneric.Aux[T, R], keys: Keys.Aux[R, K],
    values: Values.Aux[R, V],
    ktl: ToList[K, Symbol],
    vtl: ToList[V, Any]
  ) = {
    //age(15):::name(nt) age,name 15,nt
    val fieldNames = keys().toList.map(_.name)
    val lines = ys.map { y =>
      gen.to(y).values.toList.map{x=>
        getValue(x)
      }
    }
    fieldNames :: lines
  }

  def jsonToMap(json:String) = {
    JSON.parseFull(json).get.asInstanceOf[Map[String, String]]
  }

  def jsonToMapSeq(json:String) = {
    JSON.parseFull(json).get.asInstanceOf[Seq[Map[String, String]]]
  }

  def jsonToMapDouble(json:String) = {
    JSON.parseFull(json).get.asInstanceOf[Map[String, Double]]
  }

  def mapToJson(map:Map[String,String]) = {
    Json.toJson(map).toString()
  }

  def stringToJson(map:String) = {
    Json.toJson(map).toString()
  }

//  def getJsonByT[T](y: T) = {
//    val map = y.getClass.getDeclaredFields.toList.map { x: Field =>
//      x.setAccessible(true)
//      val kind = x.get(y)
//      val value = getValue(kind, "")
//      (x.getName, value)
//    }.toMap
//    Json.toJson(map)
//  }


  def date : String = {
    val now = new Date()
    val dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
    val date = dateFormat.format(now)
    date
  }

  def getUserId[T](request:Request[T]):Int = {
    request.session.get("id").get.toInt
  }


  def getTime(startTime: Long) = {
    val endTime = System.currentTimeMillis()
    (endTime - startTime) / 1000.0
  }

}
