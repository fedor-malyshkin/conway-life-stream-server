package ru.fedor.conway.life.stream.server

import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior, PostStop, Signal}
import ru.fedor.conway.life.stream.server.FieldController.FieldControllerMessage
import ru.fedor.conway.life.stream.server.ServerController.ServerControllerMessage
import ru.fedor.conway.life.stream.server.StatePublisher.StatePublisherMessage

object ServerController {
  def apply(): Behavior[ServerControllerMessage] =
    Behaviors.setup[ServerControllerMessage] { context =>
      new ServerController(context,
        context.spawn(FieldController(), "field-controller"),
        context.spawn(StatePublisher(), "state-publisher"))
    }

  sealed trait ServerControllerMessage

  object ServerControllerPoisonPill extends ServerControllerMessage
}

class ServerController(context: ActorContext[ServerControllerMessage],
                       fieldController: ActorRef[FieldControllerMessage],
                       statePublisher: ActorRef[StatePublisherMessage]) extends AbstractBehavior[ServerControllerMessage](context) {

  import ru.fedor.conway.life.stream.server.FieldController._
  import ru.fedor.conway.life.stream.server.StatePublisher._

  context.log.info(s"${Server.SERVER_NAME} started")

  statePublisher ! FieldControllerSubscribe(fieldController)

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