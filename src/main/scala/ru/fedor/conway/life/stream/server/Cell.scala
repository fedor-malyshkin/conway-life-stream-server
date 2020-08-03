package ru.fedor.conway.life.stream.server

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import ru.fedor.conway.life.stream.server.Cell.CellMessage


object Cell {
  def apply(): Behavior[CellMessage] =
    Behaviors.setup[CellMessage](context => new Cell(context))

  sealed trait CellMessage

}

class Cell(context: ActorContext[CellMessage]) extends AbstractBehavior[CellMessage](context) {
  override def onMessage(msg: CellMessage): Behavior[CellMessage] = ???
}