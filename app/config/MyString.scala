package config

trait MyString {


  implicit class myString(str: String) {

    def isDouble: Boolean = {
      try {
        str.toDouble
      } catch {
        case e: Exception => return false
      }
      true
    }
  }

}
