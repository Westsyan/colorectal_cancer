package test

import config.{ConvertHelper, MigrationTool, MyListTool}



object Test extends MigrationTool with MyListTool {

  def main(args: Array[String]): Unit = {

    case class Person(age: Int, name: String, names: List[String])
    case class Person1(age: Int, name: String)

        val data=List(Person(15,"nt",List("a","b")),
          Person(16,"nt",List("a","b")),
          Person(16,"nt",List("a","c"))
        )
        val rs=data.lines
        println(rs)
    val map = Map("age" -> "15", "name" -> "yz").map { case (k, v) =>
      if (k == "age") {
        (k, v.toInt)
      } else (k, v)
    }
   // val rs = new ConvertHelper[Person1].from(map).get
    println(rs)



    //    val json=Utils.getJsonByT(data)
    //    println(json)


  }

}
