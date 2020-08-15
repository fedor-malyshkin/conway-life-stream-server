package ru.fedor.conway.life.stream.server

import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior, PostStop, Signal}
import ru.fedor.conway.life.stream.server.Cell.CellState
import ru.fedor.conway.life.stream.server.Field.{FieldMessage, GameStart}
import ru.fedor.conway.life.stream.server.FieldController.{FieldControllerMessage, FieldStateGenerated, createField}
import ru.fedor.conway.life.stream.server.FieldStateGenerator.{FieldStateGenerate, FieldStateMessage}


object FieldController {

  sealed trait FieldControllerMessage

  object GameEnded extends FieldControllerMessage

  object GameTurnEnded extends FieldControllerMessage

  object GameStart extends FieldControllerMessage

  case class FieldStateGenerated(fieldState: Map[CellId, CellState]) extends FieldControllerMessage

  case class FieldStateEvent(cellId: CellId, cellState: CellState) extends FieldControllerMessage

  def apply(): Behavior[FieldControllerMessage] =
    Behaviors.setup[FieldControllerMessage] { context =>
      new FieldController(context,
        createField(context),
        context.spawn(FieldStateGenerator(), "field-state-generator"))
    }

  def createField(context: ActorContext[FieldControllerMessage]): ActorRef[FieldMessage] = context.spawnAnonymous(Field())
}

class FieldController(context: ActorContext[FieldControllerMessage],
                      field: ActorRef[FieldMessage],
                      fieldStateGenerator: ActorRef[FieldStateMessage]) extends AbstractBehavior[FieldControllerMessage](context) {

  override def onMessage(msg: FieldControllerMessage): Behavior[FieldControllerMessage] =
    msg match {
      case FieldController.GameStart | FieldController.GameEnded =>
        fieldStateGenerator ! FieldStateGenerate(context.self)
        Behaviors.same
      case FieldStateGenerated(fieldState) =>
        val newField = createField(context)
        newField ! GameStart(fieldState, context.self)
        new FieldController(context, newField, fieldStateGenerator)
      case FieldController.GameTurnEnded =>
        Behaviors.same
      case FieldController.FieldStateEvent(cellId, cellState) =>
        Behaviors.same
    }

  override def onSignal: PartialFunction[Signal, Behavior[FieldControllerMessage]] = {
    case PostStop =>
      context.log.info(s"${Server.SERVER_NAME} stopped")
      this
  }
}