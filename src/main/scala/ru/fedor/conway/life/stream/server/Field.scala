package ru.fedor.conway.life.stream.server

import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}
import ru.fedor.conway.life.stream.server.Field.FieldMessage

object Field {
  def apply(fieldState: ActorRef[FieldState.FieldStateMessage]): Behavior[FieldMessage] =
    Behaviors.setup[FieldMessage] { context =>
      new Field(fieldState, context)
    }

  sealed trait FieldMessage

}

class Field(fieldState: ActorRef[FieldState.FieldStateMessage], context: ActorContext[FieldMessage]) extends AbstractBehavior[FieldMessage](context) {

  override def onMessage(msg: FieldMessage): Behavior[FieldMessage] = {
    // No need to handle any messages
    Behaviors.unhandled
  }
}
