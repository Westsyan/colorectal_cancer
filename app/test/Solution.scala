package test

object Solution {


  def main(args: Array[String]): Unit = {

    val l = "cqabad"


    var text: Array[Char] = Array()
    var max = 0
    var maxText = ""

    l.foreach { x =>
      text :+= x
      if (text.distinct.length == text.length) {
        if (max < text.length) {
          max = text.length
          maxText = text.mkString
        }
      } else {
        text = text.drop(text.indexOf(x)+1)
      }
    }

    println(max)
    println(maxText)

  }


  def x: Array[Int] = {
    val nums = Array(2, 7, 11, 15)
    val target = 9
    var result: Array[Int] = Array()

    nums.zipWithIndex.find { x =>
      val index2 = nums.indexOf(target - x._1)
      if (index2 != -1 && x._2 != index2) {
        result = Array(x._2, index2)
        true
      } else false
    }
    result
  }

  def lastRemaining(n: Int): Int = {
    var count = 1
    var array = (1 to n).map(x => x).toArray.zipWithIndex
    while (array.length != 1) {
      if (count == 1) {
        array = array.filter(_._2 % 2 == 1).map(_._1).reverse.zipWithIndex
        count = 2
      } else {
        array = array.filter(_._2 % 2 == 1).map(_._1).reverse.zipWithIndex
        count = 1
      }
    }
    array.head._1
  }

  def isHappy(n: Int): Boolean = {

    var sum = n
    var sumList = Array[Int]()
    while (sum != 1) {
      sum = sum.toString.map(_.toString.toInt).map(x => x * x).sum
      sumList = sumList :+ sum
      if (sumList.count(_ == sum) == 2) {
        return false
      }
    }
    true
  }
}