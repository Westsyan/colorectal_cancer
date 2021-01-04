package dao

import javax.inject.Inject
import models.Tables._
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile

import scala.concurrent.{ExecutionContext, Future}

class SampleDao@Inject()(protected val dbConfigProvider: DatabaseConfigProvider)
                        (implicit exec:ExecutionContext)extends
  HasDatabaseConfigProvider[JdbcProfile]  {


  import profile.api._

  def insert(row:Seq[SampleRow]) : Future[Unit] ={
    db.run(Sample ++= row).map(_=>())
  }

  def getAllSample : Future[Seq[SampleRow]] = {
    db.run(Sample.result)
  }


}
