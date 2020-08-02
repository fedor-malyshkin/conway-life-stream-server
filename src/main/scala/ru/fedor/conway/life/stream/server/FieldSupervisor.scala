package ru.fedor.conway.life.stream.server

import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior, PostStop, Signal}


object FieldSupervisor {
  def apply(): Behavior[Nothing] =
    Behaviors.setup[Nothing] { context =>
      val fieldState = context.spawn(FieldState(), "field-state")
      val field = context.spawn(Field(fieldState), "field")
      new FieldSupervisor(field, fieldState, context)
    }
}

class FieldSupervisor(field: ActorRef[Field.FieldMessage],
                      fieldState: ActorRef[FieldState.FieldStateMessage],
                      context: ActorContext[Nothing]) extends AbstractBehavior[Nothing](context) {
  context.log.info(s"${Server.SERVER_NAME} started")
  println(s"${Server.SERVER_NAME} started")


  override def onMessage(msg: Nothing): Behavior[Nothing] = {
    Behaviors.unhandled
  }

  override def onSignal: PartialFunction[Signal, Behavior[Nothing]] = {
    case PostStop =>
      context.log.info(s"${Server.SERVER_NAME} stopped")
      this
  }
}