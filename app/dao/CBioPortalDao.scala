package dao

import javax.inject.Inject
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile
import models.Tables._

import scala.concurrent.{ExecutionContext, Future}

class CBioPortalDao @Inject()(protected val dbConfigProvider: DatabaseConfigProvider)
                             (implicit exec: ExecutionContext) extends
  HasDatabaseConfigProvider[JdbcProfile] {

  import profile.api._

  def insert(row: Seq[CbioportalfilesRow]): Future[Unit] = {
    db.run(Cbioportalfiles ++= row).map(_ => ())
  }

  def getByName(name: String): Future[Seq[CbioportalfilesRow]] = {
    db.run(Cbioportalfiles.filter(_.f1 === name).result)
  }
}
