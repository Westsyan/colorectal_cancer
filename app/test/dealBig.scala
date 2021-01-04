package test

import config.MyFile
import org.apache.commons.io.FileUtils
import scala.jdk.CollectionConverters._


object dealBig extends MyFile{

  def main(args: Array[String]): Unit = {
    val buffer = "D:\\结直肠癌诊疗系统/big.txt".readLines.filter(x=>x.indexOf("http://ncov-ai.big.ac.cn:80/download") != -1).map{x=>
      val i = x.indexOf("http://ncov-ai.big.ac.cn:80/download")
     val line = x.drop(i)
      val i2 = line.indexOf("\"")
      val href = line.take(i2)
      s"wget -c $href"
    }

    FileUtils.writeLines("D:\\结直肠癌诊疗系统/bigDownload.sh".toFile,buffer.asJava)
  }
}
