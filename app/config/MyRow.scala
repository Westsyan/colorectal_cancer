package config

import org.joda.time.DateTime
import play.api.libs.json.{JsValue, Json}
import shapeless._
import shapeless.ops.record._
import shapeless.record._

trait MyRow {

  implicit class MyRow[T, R <: HList](row: T) {

    def getMapByT()(implicit gen: LabelledGeneric.Aux[T, R],
                    toMap: ToMap.Aux[R, Symbol, Any]): Map[String, String] = {
      gen.to(row).toMap.map { case (symbol, value) =>
        (symbol.name, getValue(value))
      }
    }

    def getJsonByT()(implicit gen: LabelledGeneric.Aux[T, R],
                     toMap: ToMap.Aux[R, Symbol, Any]): JsValue = {
      Json.toJson(getMapByT())
    }
  }


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

}
