package test

import config.MyFile
import org.apache.commons.io.FileUtils
import scala.jdk.CollectionConverters._

object test1 extends MyFile {

  def main(args: Array[String]): Unit = {

    val test = "D:\\结直肠癌诊疗系统\\cancer123\\genes/test.txt".readLines.map{x=>
      val l = x.split("\t")
      (l.head.trim,x)
    }.toMap

    val genes = "D:\\结直肠癌诊疗系统\\cancer123\\genes/genes.txt".readLines.map{x=>
      val l = x.split("\t")
      println(l.head)

      test(l.head.trim) + "\t" + l.tail.mkString("\t")
    }

    FileUtils.writeLines("D:\\结直肠癌诊疗系统\\cancer123\\genes/t.txt".toFile,genes.asJava)



  }

  def getGenes = {

    val row = "D:\\结直肠癌诊疗系统\\cancer123/新建文本文档 (2).txt".readFileToString

    val r = row.split("</tr>").map { x =>
      val lines = x.split("</td>")
      val head = lines.head
      val name = head.split(">").init.last.dropRight(3)
      val href = head.slice(head.indexOf("'")+1,head.lastIndexOf("'"))
      val ill = (lines(1)+" ").split(">").last.trim
      val drug = (lines(2)+" ").split(">").last.trim
      val drug2 = (lines(3)+" ").split(">").last.trim
      val genes = (lines(4)+" ").split(">").last.trim
      val explain = (lines(5)+" ").split(">").last.trim

      Array(name,href,ill,drug,drug2,genes,explain).mkString("\t")
    }

    FileUtils.writeLines("D:\\结直肠癌诊疗系统\\cancer123/genes.txt".toFile, r.toBuffer.asJava)




  }


  def getDrug = {
    val row = "D:\\结直肠癌诊疗系统/cancer.txt".readLines.map(_.trim).filter(_ != "").map { x =>
      val index = x.indexOf("</i></a><p class=\"lite-xcx-ewm\">")
      val text = x.take(index).split("<i>")
      val chinese = text.last
      val text2 = text.head.split("\" class=\"disease_name\" target=\"_blank\">")
      val english = text2.last
      val href = text2.head.drop(13)
      english + "\t" + chinese + "\t" + href
    }
    FileUtils.writeLines("D:\\结直肠癌诊疗系统/cancer2.txt".toFile, row.asJava)

  }

}
