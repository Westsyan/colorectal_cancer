package models
// AUTO-GENERATED Slick data model
/** Stand-alone Slick data model for immediate use */
object Tables extends {
  val profile = slick.jdbc.MySQLProfile
} with Tables

/** Slick data model trait for extension, choice of backend or usage in the cake pattern. (Make sure to initialize this late.) */
trait Tables {
  val profile: slick.jdbc.JdbcProfile
  import profile.api._
  import com.github.tototoshi.slick.MySQLJodaSupport._
  import org.joda.time.DateTime
  import slick.model.ForeignKeyAction
  // NOTE: GetResult mappers for plain SQL are only generated for tables where Slick knows how to map the types of all columns.
  import slick.jdbc.{GetResult => GR}

  /** DDL for all tables. Call .create to execute. */
  lazy val schema: profile.SchemaDescription = Cbioportalfiles.schema ++ Ml.schema ++ Sample.schema ++ Tools.schema ++ User.schema
  @deprecated("Use .schema instead of .ddl", "3.0")
  def ddl = schema

  /** Entity class storing rows of table Cbioportalfiles
   *  @param id Database column id SqlType(INT), AutoInc
   *  @param f1 Database column f1 SqlType(VARCHAR), Length(255,true)
   *  @param f2 Database column f2 SqlType(VARCHAR), Length(255,true)
   *  @param meta Database column meta SqlType(TEXT) */
  case class CbioportalfilesRow(id: Int, f1: String, f2: String, meta: String)
  /** GetResult implicit for fetching CbioportalfilesRow objects using plain SQL queries */
  implicit def GetResultCbioportalfilesRow(implicit e0: GR[Int], e1: GR[String]): GR[CbioportalfilesRow] = GR{
    prs => import prs._
    CbioportalfilesRow.tupled((<<[Int], <<[String], <<[String], <<[String]))
  }
  /** Table description of table cbioportalfiles. Objects of this class serve as prototypes for rows in queries. */
  class Cbioportalfiles(_tableTag: Tag) extends profile.api.Table[CbioportalfilesRow](_tableTag, Some("colorectal_cancer"), "cbioportalfiles") {
    def * = (id, f1, f2, meta) <> (CbioportalfilesRow.tupled, CbioportalfilesRow.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = ((Rep.Some(id), Rep.Some(f1), Rep.Some(f2), Rep.Some(meta))).shaped.<>({r=>import r._; _1.map(_=> CbioportalfilesRow.tupled((_1.get, _2.get, _3.get, _4.get)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column id SqlType(INT), AutoInc */
    val id: Rep[Int] = column[Int]("id", O.AutoInc)
    /** Database column f1 SqlType(VARCHAR), Length(255,true) */
    val f1: Rep[String] = column[String]("f1", O.Length(255,varying=true))
    /** Database column f2 SqlType(VARCHAR), Length(255,true) */
    val f2: Rep[String] = column[String]("f2", O.Length(255,varying=true))
    /** Database column meta SqlType(TEXT) */
    val meta: Rep[String] = column[String]("meta")

    /** Primary key of Cbioportalfiles (database name cbioportalfiles_PK) */
    val pk = primaryKey("cbioportalfiles_PK", (id, f1))
  }
  /** Collection-like TableQuery object for table Cbioportalfiles */
  lazy val Cbioportalfiles = new TableQuery(tag => new Cbioportalfiles(tag))

  /** Entity class storing rows of table Ml
   *  @param id Database column id SqlType(INT), AutoInc
   *  @param userid Database column userid SqlType(INT)
   *  @param name Database column name SqlType(VARCHAR), Length(255,true)
   *  @param ml Database column ml SqlType(VARCHAR), Length(255,true)
   *  @param start Database column start SqlType(VARCHAR), Length(255,true)
   *  @param end Database column end SqlType(VARCHAR), Length(255,true)
   *  @param state Database column state SqlType(INT) */
  case class MlRow(id: Int, userid: Int, name: String, ml: String, start: String, end: String, state: Int)
  /** GetResult implicit for fetching MlRow objects using plain SQL queries */
  implicit def GetResultMlRow(implicit e0: GR[Int], e1: GR[String]): GR[MlRow] = GR{
    prs => import prs._
    MlRow.tupled((<<[Int], <<[Int], <<[String], <<[String], <<[String], <<[String], <<[Int]))
  }
  /** Table description of table ml. Objects of this class serve as prototypes for rows in queries. */
  class Ml(_tableTag: Tag) extends profile.api.Table[MlRow](_tableTag, Some("colorectal_cancer"), "ml") {
    def * = (id, userid, name, ml, start, end, state) <> (MlRow.tupled, MlRow.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = ((Rep.Some(id), Rep.Some(userid), Rep.Some(name), Rep.Some(ml), Rep.Some(start), Rep.Some(end), Rep.Some(state))).shaped.<>({r=>import r._; _1.map(_=> MlRow.tupled((_1.get, _2.get, _3.get, _4.get, _5.get, _6.get, _7.get)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column id SqlType(INT), AutoInc */
    val id: Rep[Int] = column[Int]("id", O.AutoInc)
    /** Database column userid SqlType(INT) */
    val userid: Rep[Int] = column[Int]("userid")
    /** Database column name SqlType(VARCHAR), Length(255,true) */
    val name: Rep[String] = column[String]("name", O.Length(255,varying=true))
    /** Database column ml SqlType(VARCHAR), Length(255,true) */
    val ml: Rep[String] = column[String]("ml", O.Length(255,varying=true))
    /** Database column start SqlType(VARCHAR), Length(255,true) */
    val start: Rep[String] = column[String]("start", O.Length(255,varying=true))
    /** Database column end SqlType(VARCHAR), Length(255,true) */
    val end: Rep[String] = column[String]("end", O.Length(255,varying=true))
    /** Database column state SqlType(INT) */
    val state: Rep[Int] = column[Int]("state")

    /** Primary key of Ml (database name ml_PK) */
    val pk = primaryKey("ml_PK", (id, userid))
  }
  /** Collection-like TableQuery object for table Ml */
  lazy val Ml = new TableQuery(tag => new Ml(tag))

  /** Entity class storing rows of table Sample
   *  @param id Database column id SqlType(INT), AutoInc, PrimaryKey
   *  @param samples Database column samples SqlType(TEXT) */
  case class SampleRow(id: Int, samples: String)
  /** GetResult implicit for fetching SampleRow objects using plain SQL queries */
  implicit def GetResultSampleRow(implicit e0: GR[Int], e1: GR[String]): GR[SampleRow] = GR{
    prs => import prs._
    SampleRow.tupled((<<[Int], <<[String]))
  }
  /** Table description of table sample. Objects of this class serve as prototypes for rows in queries. */
  class Sample(_tableTag: Tag) extends profile.api.Table[SampleRow](_tableTag, Some("colorectal_cancer"), "sample") {
    def * = (id, samples) <> (SampleRow.tupled, SampleRow.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = ((Rep.Some(id), Rep.Some(samples))).shaped.<>({r=>import r._; _1.map(_=> SampleRow.tupled((_1.get, _2.get)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column id SqlType(INT), AutoInc, PrimaryKey */
    val id: Rep[Int] = column[Int]("id", O.AutoInc, O.PrimaryKey)
    /** Database column samples SqlType(TEXT) */
    val samples: Rep[String] = column[String]("samples")
  }
  /** Collection-like TableQuery object for table Sample */
  lazy val Sample = new TableQuery(tag => new Sample(tag))

  /** Entity class storing rows of table Tools
   *  @param id Database column id SqlType(INT), AutoInc
   *  @param userid Database column userid SqlType(INT)
   *  @param name Database column name SqlType(VARCHAR), Length(255,true)
   *  @param tool Database column tool SqlType(VARCHAR), Length(255,true)
   *  @param start Database column start SqlType(VARCHAR), Length(255,true)
   *  @param end Database column end SqlType(VARCHAR), Length(255,true)
   *  @param state Database column state SqlType(INT)
   *  @param params Database column params SqlType(TEXT)
   *  @param drawparams Database column drawparams SqlType(TEXT) */
  case class ToolsRow(id: Int, userid: Int, name: String, tool: String, start: String, end: String, state: Int, params: String, drawparams: String)
  /** GetResult implicit for fetching ToolsRow objects using plain SQL queries */
  implicit def GetResultToolsRow(implicit e0: GR[Int], e1: GR[String]): GR[ToolsRow] = GR{
    prs => import prs._
    ToolsRow.tupled((<<[Int], <<[Int], <<[String], <<[String], <<[String], <<[String], <<[Int], <<[String], <<[String]))
  }
  /** Table description of table tools. Objects of this class serve as prototypes for rows in queries. */
  class Tools(_tableTag: Tag) extends profile.api.Table[ToolsRow](_tableTag, Some("colorectal_cancer"), "tools") {
    def * = (id, userid, name, tool, start, end, state, params, drawparams) <> (ToolsRow.tupled, ToolsRow.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = ((Rep.Some(id), Rep.Some(userid), Rep.Some(name), Rep.Some(tool), Rep.Some(start), Rep.Some(end), Rep.Some(state), Rep.Some(params), Rep.Some(drawparams))).shaped.<>({r=>import r._; _1.map(_=> ToolsRow.tupled((_1.get, _2.get, _3.get, _4.get, _5.get, _6.get, _7.get, _8.get, _9.get)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column id SqlType(INT), AutoInc */
    val id: Rep[Int] = column[Int]("id", O.AutoInc)
    /** Database column userid SqlType(INT) */
    val userid: Rep[Int] = column[Int]("userid")
    /** Database column name SqlType(VARCHAR), Length(255,true) */
    val name: Rep[String] = column[String]("name", O.Length(255,varying=true))
    /** Database column tool SqlType(VARCHAR), Length(255,true) */
    val tool: Rep[String] = column[String]("tool", O.Length(255,varying=true))
    /** Database column start SqlType(VARCHAR), Length(255,true) */
    val start: Rep[String] = column[String]("start", O.Length(255,varying=true))
    /** Database column end SqlType(VARCHAR), Length(255,true) */
    val end: Rep[String] = column[String]("end", O.Length(255,varying=true))
    /** Database column state SqlType(INT) */
    val state: Rep[Int] = column[Int]("state")
    /** Database column params SqlType(TEXT) */
    val params: Rep[String] = column[String]("params")
    /** Database column drawparams SqlType(TEXT) */
    val drawparams: Rep[String] = column[String]("drawparams")

    /** Primary key of Tools (database name tools_PK) */
    val pk = primaryKey("tools_PK", (id, userid, tool))
  }
  /** Collection-like TableQuery object for table Tools */
  lazy val Tools = new TableQuery(tag => new Tools(tag))

  /** Entity class storing rows of table User
   *  @param id Database column id SqlType(INT), AutoInc
   *  @param user Database column user SqlType(VARCHAR), Length(255,true)
   *  @param pwd Database column pwd SqlType(VARCHAR), Length(255,true) */
  case class UserRow(id: Int, user: String, pwd: String)
  /** GetResult implicit for fetching UserRow objects using plain SQL queries */
  implicit def GetResultUserRow(implicit e0: GR[Int], e1: GR[String]): GR[UserRow] = GR{
    prs => import prs._
    UserRow.tupled((<<[Int], <<[String], <<[String]))
  }
  /** Table description of table user. Objects of this class serve as prototypes for rows in queries. */
  class User(_tableTag: Tag) extends profile.api.Table[UserRow](_tableTag, Some("colorectal_cancer"), "user") {
    def * = (id, user, pwd) <> (UserRow.tupled, UserRow.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = ((Rep.Some(id), Rep.Some(user), Rep.Some(pwd))).shaped.<>({r=>import r._; _1.map(_=> UserRow.tupled((_1.get, _2.get, _3.get)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column id SqlType(INT), AutoInc */
    val id: Rep[Int] = column[Int]("id", O.AutoInc)
    /** Database column user SqlType(VARCHAR), Length(255,true) */
    val user: Rep[String] = column[String]("user", O.Length(255,varying=true))
    /** Database column pwd SqlType(VARCHAR), Length(255,true) */
    val pwd: Rep[String] = column[String]("pwd", O.Length(255,varying=true))

    /** Primary key of User (database name user_PK) */
    val pk = primaryKey("user_PK", (id, user))
  }
  /** Collection-like TableQuery object for table User */
  lazy val User = new TableQuery(tag => new User(tag))
}
