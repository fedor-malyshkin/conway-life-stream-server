package ru.fedor.conway.life.stream.server

import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import akka.actor.typed.{Behavior, PostStop, Signal}


object FieldSupervisor {
  def apply(): Behavior[Nothing] =
    Behaviors.setup[Nothing](context => new FieldSupervisor(context))
}

class FieldSupervisor(context: ActorContext[Nothing]) extends AbstractBehavior[Nothing](context) {
  context.log.info(s"$Server.SERVER_NAME started")


  override def onMessage(msg: Nothing): Behavior[Nothing] = {
    Behaviors.unhandled
  }

  override def onSignal: PartialFunction[Signal, Behavior[Nothing]] = {
    case PostStop =>
      context.log.info(s"$Server.SERVER_NAME stopped")
      this
  }
}