package ru.fedor.conway.life.stream.server

import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import akka.actor.typed.{Behavior, PostStop, Signal}
import ru.fedor.conway.life.stream.server.FieldState.FieldStateMessage


object FieldState {
  def apply(): Behavior[FieldStateMessage] =
    Behaviors.setup[FieldStateMessage](context => new FieldState(context))

  sealed trait FieldStateMessage

}

class FieldState(context: ActorContext[FieldStateMessage]) extends AbstractBehavior[FieldStateMessage](context) {

  override def onMessage(msg: FieldStateMessage): Behavior[FieldStateMessage] = {
    // No need to handle any messages
    Behaviors.unhandled
  }

  override def onSignal: PartialFunction[Signal, Behavior[FieldStateMessage]] = {
    case PostStop =>
      this
  }
}