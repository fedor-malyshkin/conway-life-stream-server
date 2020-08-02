package ru.fedor.conway.life.stream.server

import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import akka.actor.typed.{Behavior, PostStop, Signal}


object FieldState {
  def apply(): Behavior[Nothing] =
    Behaviors.setup[Nothing](context => new FieldState(context))
}

class FieldState(context: ActorContext[Nothing]) extends AbstractBehavior[Nothing](context) {

  override def onMessage(msg: Nothing): Behavior[Nothing] = {
    // No need to handle any messages
    Behaviors.unhandled
  }

  override def onSignal: PartialFunction[Signal, Behavior[Nothing]] = {
    case PostStop =>
      this
  }
}