package dao

import javax.inject.Inject
import models.Tables._
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile

import scala.concurrent.{ExecutionContext, Future}

class UserDao@Inject()(protected val dbConfigProvider: DatabaseConfigProvider)
                      (implicit exec:ExecutionContext)extends
  HasDatabaseConfigProvider[JdbcProfile]   {

  import profile.api._


  def addUser(user:UserRow) : Future[Int] = {
    db.run(User returning User.map(_.id) += user)
  }

  def checkPwd(user:String,pwd:String) : Future[Boolean] = {
    db.run(User.filter(_.user === user).filter(_.pwd === pwd).exists.result)
  }

  def getUserId(user:String,pwd:String) : Future[Int] = {
    db.run(User.filter(_.user === user).filter(_.pwd === pwd).map(_.id).result.head)
  }

  def checkUser(user:String) : Future[Boolean] = {
    db.run(User.filter(_.user === user).exists.result)
  }

}
