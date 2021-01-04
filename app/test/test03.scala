package test

object test03 {

  def main(args: Array[String]): Unit = {
    val x = "鳞癌相关抗原（ng/mL）\t0.00-1.50\n铁蛋白（SF）（ng/mL）\t15.8-367.1"

    x.split("\n").map{y=>
     val l =  y.split("\t")
      val min = l.last.split("-").head
      val max = l.last.split("-").last

      println(s"""{"name":"${l.head}","type":"between","range":[{"min":"$min","max":"$max"}]},""")

    }
  }
}
