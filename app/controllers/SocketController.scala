package controllers

import akka.actor.{Actor, ActorSystem, PoisonPill, Props}
import akka.stream.Materializer
import config.MyAwait
import dao.{MlDao, ToolsDao}
import javax.inject.Inject
import play.api.libs.json.JsValue
import play.api.libs.streams.ActorFlow
import play.api.mvc.WebSocket.MessageFlowTransformer
import play.api.mvc.{AbstractController, ControllerComponents, WebSocket}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.language.postfixOps

class SocketController @Inject()(cc: ControllerComponents,
                                 mlDao: MlDao,
                                 toolsDao:ToolsDao)(implicit system: ActorSystem, ec: ExecutionContext, mat: Materializer)
  extends AbstractController(cc) with MyAwait{

  case class TaskData(taskInfo:(Int,Seq[Int]),tools:String)

  implicit val messageFlowTransformer: MessageFlowTransformer[JsValue, String] =
    MessageFlowTransformer.jsonMessageFlowTransformer[JsValue, String]

  def socket :WebSocket = WebSocket.accept[JsValue, String] {implicit request =>
    val userid =  request.session.get("id").get.toInt
    ActorFlow.actorRef { out =>
      Props(new Actor{
        override def receive: Receive = {
          case msg :JsValue if (msg \ "info").as[String] == "start" =>
            val t = (msg \ "t").as[String]
            val taskInfo = getTaskInfo(userid,t)
            system.scheduler.scheduleOnce(3 seconds, self, TaskData(taskInfo,t))
          case TaskData(taskInfo,tools) =>
            val taskInfoNow = getTaskInfo(userid,tools)
            if(taskInfo._1 != taskInfoNow._1 || taskInfo._2.diff(taskInfoNow._2).nonEmpty){
              out ! "update"
            }
            system.scheduler.scheduleOnce(3 seconds, self, TaskData(taskInfoNow,tools))
          case _=>
            self ! PoisonPill
        }
        override def postStop(): Unit = {
          self ! PoisonPill
        }
      })
    }
  }


  /**
    *
    * @param id 用户ID
    * @param t 任务类型
    * @return  int: 任务数量，seq[Int]：任务状态集合
    */
  def getTaskInfo(id:Int,t:String):(Int,Seq[Int]) = {
    t match {
      case x if Array("rf","logistics","svm","neural","gb","catboost").contains(x) =>
        val task = mlDao.getByUseridAndMl(id, x).toAwait
        (task.length, task.map(_.state))
      case x if Array("pca","pcoa","cca","heatmap","igc","itc","tax4","volcano","fh","ternary","treemap","lefse").contains(x) =>
        val task = toolsDao.getByUseridAndTools(id, x).toAwait
        (task.length, task.map(_.state))
    }
  }


}
