package test

import config.{MyFile, MyString}

object test5 extends MyFile with MyString{

  def main(args: Array[String]): Unit = {


    val groupdata=Map("gcolor[]"->"#FF0000:#0000FF")

    println(groupdata("gcolor[]"))

  }
}
