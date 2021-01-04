package test

import config.{MyFile, MyString}

object test5 extends MyFile with MyString{

  def main(args: Array[String]): Unit = {


    val h = "D:\\结直肠癌诊疗系统/2020-10-12 奥沙利铂耐药与敏感组.txt".readLines

    h.foreach{x=>
     println( x.split("\t").count(_.isDouble))


    }

  }
}
