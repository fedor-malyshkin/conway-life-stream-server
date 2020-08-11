package ru.fedor.conway.life.stream.server

import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior, PostStop, Signal}
import akka.http.scaladsl.model.ContentTypes
import akka.http.scaladsl.server.Directives
import akka.http.scaladsl.server.Directives.{complete, path}
import akka.http.scaladsl.{Http, model}
import akka.stream.ActorMaterializer
import com.typesafe.config.{Config, ConfigFactory}
import ru.fedor.conway.life.stream.server.FieldController.{FieldControllerMessage, GameStart}
import ru.fedor.conway.life.stream.server.ServerController.ServerControllerMessage

object ServerController {
  def apply(): Behavior[ServerControllerMessage] =
    Behaviors.setup[ServerControllerMessage] { context =>
      new ServerController(context, context.spawn(FieldController(), "field-controller"))
    }

  sealed trait ServerControllerMessage

  object ServerControllerPoisonPill extends ServerControllerMessage

}

class ServerController(context: ActorContext[ServerControllerMessage],
                       fieldController: ActorRef[FieldControllerMessage]) extends AbstractBehavior[ServerControllerMessage](context) {
  context.log.info(s"${Server.SERVER_NAME} started")

  implicit val untypedSystem = context.system.classicSystem

  implicit val materializer = ActorMaterializer()
  // needed for the future flatMap/onComplete in the end
  implicit val executionContext = untypedSystem.dispatcher

  val route =
    path("") {
      Directives.get {
        complete(model.HttpEntity(ContentTypes.`text/html(UTF-8)`, "<h1>Say hello to akka-http</h1>"))
      }
    }

  val conf: Config = ConfigFactory.load()

  val bindingFuture = Http().newServerAt(
    conf.getString(s"${Server.CONF_ROOT}.bind-host"),
    conf.getInt(s"${Server.CONF_ROOT}.bind-port")).bindFlow(route)

  fieldController ! GameStart

  override def onMessage(msg: ServerControllerMessage): Behavior[ServerControllerMessage] =
    msg match {
      case ServerController.ServerControllerPoisonPill =>
        Behaviors.stopped
    }

  override def onSignal: PartialFunction[Signal, Behavior[ServerControllerMessage]] = {
    case PostStop =>
      context.log.info(s"${Server.SERVER_NAME} stopped")
      this
  }
}