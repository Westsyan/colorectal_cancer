package test

import config.{MyConfig, MyListTool}
import utils.ReadExcel

import scala.collection.mutable

object test02  extends MyConfig with  MyListTool{

  def main(args: Array[String]): Unit = {

    val lines =  ReadExcel.xlsx2Lines("D:\\结直肠癌诊疗系统/sample.xlsx".toFile)


    val w = lines.map(_.split("\t").toList).toList


    val head = w.head


   val wq =  w.tail.map{x=>
      val w : mutable.LinkedHashMap[String,String] = mutable.LinkedHashMap()
      x.zipWithIndex.map{y=>
        w.put(head(y._2),y._1)
      }

     println(w)
    }









  }
}
