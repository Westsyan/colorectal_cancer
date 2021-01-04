package dao

import javax.inject.Inject
import models.Tables._
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile

import scala.concurrent.{ExecutionContext, Future}

class MlDao@Inject()(protected val dbConfigProvider: DatabaseConfigProvider)
                    (implicit exec:ExecutionContext)extends
  HasDatabaseConfigProvider[JdbcProfile]   {

  import profile.api._

  def insertReturnId(ml:MlRow) : Future[Int] = {
    db.run(Ml returning Ml.map(_.id) += ml)
  }

  def getByMlAndUserid(userid:Int,ml:String) : Future[Seq[MlRow]] = {
    db.run(Ml.filter(_.userid === userid).filter(_.ml === ml).result)
  }

  def getSuccessByMlAndUserid(userid:Int,ml:String) : Future[Seq[MlRow]] = {
    db.run(Ml.filter(_.userid === userid).filter(_.ml === ml).filter(_.state === 1).result)
  }

  def updateStateById(id:Int,state:Int) : Future[Unit] = {
    db.run(Ml.filter(_.id === id).map(_.state).update(state)).map(_=>())
  }

  def checkName(ml: String,id:Int, name: String): Future[Boolean] = {
    db.run(Ml.filter(_.userid === id).filter(_.ml === ml).filter(_.name === name).exists.result)
  }

  def getByUseridAndMl(userid:Int,ml:String) : Future[Seq[MlRow]] = {
    db.run(Ml.filter(_.userid === userid).filter(_.ml === ml).result)
  }

  def getById(id:Int) : Future[MlRow] = {
    db.run(Ml.filter(_.id === id).result.head)
  }

  def deleteById(id:Int) : Future[Unit] = {
    db.run(Ml.filter(_.id === id).delete).map(_=>())
  }

}
