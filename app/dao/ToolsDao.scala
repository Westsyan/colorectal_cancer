package dao

import javax.inject.Inject
import models.Tables._
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile
import utils.Utils

import scala.concurrent.{ExecutionContext, Future}

class ToolsDao @Inject()(protected val dbConfigProvider: DatabaseConfigProvider)
                        (implicit exec: ExecutionContext) extends
  HasDatabaseConfigProvider[JdbcProfile] {


  import profile.api._

  def addToolReturnId(row: ToolsRow): Future[Int] = {
    db.run(Tools returning Tools.map(_.id) += row)
  }

  def updateState(id:Int,state:Int): Future[Unit] = {
    db.run(Tools.filter(_.id === id).map(x=> (x.state,x.end)).update((state,Utils.date))).map(_=>())
  }

  def getByToolsAndUserId(userId:Int,tools: String) : Future[Seq[ToolsRow]] ={
    db.run(Tools.filter(_.userid === userId).filter(_.tool === tools).result)
  }

  def deleteById(id:Int) : Future[Unit] = {
    db.run(Tools.filter(_.id === id).delete).map(_=>())
  }

  def getById(id:Int) : Future[ToolsRow] = {
    db.run(Tools.filter(_.id === id).result.head)
  }

  def updateDrawParamsAndStateById(id:Int,state:Int,drawParams:String) : Future[Unit] = {
    db.run(Tools.filter(_.id === id).map(x=>(x.state,x.drawparams)).update((state,drawParams))).map(_=>())
  }

  def getByUseridAndTools(userId:Int,tools: String) : Future[Seq[ToolsRow]] = {
    db.run(Tools.filter(_.userid === userId).filter(_.tool === tools).result)
  }

  def checkName(userId:Int,tools:String,name:String) : Future[Boolean] = {
    db.run(Tools.filter(_.userid === userId).filter(_.tool === tools).filter(_.name === name).exists.result)
  }

}
