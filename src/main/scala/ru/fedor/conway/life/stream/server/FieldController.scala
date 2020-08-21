package ru.fedor.conway.life.stream.server

import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}
import ru.fedor.conway.life.stream.server.Cell.CellState
import ru.fedor.conway.life.stream.server.Field.{FieldMessage, GameStart}
import ru.fedor.conway.life.stream.server.FieldController.{FieldControllerMessage, FieldStateGenerated, createField}
import ru.fedor.conway.life.stream.server.FieldStateGenerator.{FieldStateGenerate, FieldStateMessage}


object FieldController {

  sealed trait FieldControllerMessage

  object GameEnded extends FieldControllerMessage

  object GameStart extends FieldControllerMessage

  object GameTurnEnded extends FieldControllerMessage

  case class SubscriberAdd(subscription: ActorRef[FieldControllerMessage]) extends FieldControllerMessage

  case class FieldStateGenerated(fieldState: Map[CellId, CellState]) extends FieldControllerMessage

  case class FieldStateEvent(cellId: CellId, cellState: CellState, stepLeft: Int = 0) extends FieldControllerMessage

  def apply(): Behavior[FieldControllerMessage] =
    Behaviors.setup[FieldControllerMessage] { context =>
      new FieldController(context,
        createField(context),
        context.spawn(FieldStateGenerator(), "field-state-generator"),
        Set.empty)
    }

  def createField(context: ActorContext[FieldControllerMessage]): ActorRef[FieldMessage] = context.spawnAnonymous(Field())
}

class FieldController(context: ActorContext[FieldControllerMessage],
                      field: ActorRef[FieldMessage],
                      fieldStateGenerator: ActorRef[FieldStateMessage],
                      subscribers: Set[ActorRef[FieldControllerMessage]]) extends AbstractBehavior[FieldControllerMessage](context) {

  override def onMessage(msg: FieldControllerMessage): Behavior[FieldControllerMessage] =
    msg match {
      case FieldController.SubscriberAdd(subscriber) =>
        new FieldController(context, field, fieldStateGenerator, subscribers + subscriber)
      case FieldController.GameStart | FieldController.GameEnded =>
        subscribers.foreach(_ ! msg)
        fieldStateGenerator ! FieldStateGenerate(context.self)
        Behaviors.same
      case FieldStateGenerated(fieldState) =>
        val newField = createField(context)
        newField ! GameStart(fieldState, context.self)
        new FieldController(context, newField, fieldStateGenerator, subscribers)
      case FieldController.GameTurnEnded =>
        subscribers.foreach(_ ! msg)
        Behaviors.same
      case FieldController.FieldStateEvent(_, _, _) =>
        subscribers.foreach(_ ! msg)
        Behaviors.same
    }
}