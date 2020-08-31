package ru.fedor.conway.life.stream.server

import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}
import com.typesafe.config.{Config, ConfigFactory}
import ru.fedor.conway.life.stream.server.Cell.{CellMessage, CellState}
import ru.fedor.conway.life.stream.server.Field.CellStateUpdated


object Cell {
  def apply(cellId: CellId, cellState: CellState): Behavior[CellMessage] =
    Behaviors.setup[CellMessage](context => new Cell(context, cellId, cellState))

  sealed trait CellMessage

  case class CellStateUpdate(neighbourCount: Int, reply: ActorRef[CellStateUpdated]) extends CellMessage

  sealed trait CellState

  object CellStateActive extends CellState

  case class CellStateDead(howLong: Int) extends CellState

  val conf: Config = ConfigFactory.load()
  private val DEAD_TRACK_LENGTH = conf.getInt(s"${Server.CONF_ROOT}.game.dead-track-history")

  def die(currentState: CellState): CellState =
    currentState match {
      case Cell.CellStateActive =>
        CellStateDead(0)
      case CellStateDead(howLong) =>
        CellStateDead(if (howLong + 1 > DEAD_TRACK_LENGTH) DEAD_TRACK_LENGTH else howLong + 1)
    }

  def live(currentState: CellState): CellState =
    currentState match {
      case Cell.CellStateActive =>
        currentState
      case CellStateDead(howLong) =>
        CellStateDead(if (howLong + 1 > DEAD_TRACK_LENGTH) DEAD_TRACK_LENGTH else howLong + 1)
    }

  def liveOrBorn(): CellState = Cell.CellStateActive

}

class Cell(context: ActorContext[CellMessage], cellId: CellId,
           var cellState: CellState // decrease load on GC, in val cease it's possible to recreate Behavior for each cell on each turn
          ) extends AbstractBehavior[CellMessage](context) {

  import ru.fedor.conway.life.stream.server.Cell._

  override def onMessage(msg: CellMessage): Behavior[CellMessage] = msg match {
    case Cell.CellStateUpdate(neighbourCount, replyTo) =>

      // https://en.wikipedia.org/wiki/Conway%27s_Game_of_Life#Rules
      // context.log.info(s"Got: $msg")
      val result: (CellState, Boolean) =
        if (neighbourCount == 2) {
          val newState = live(cellState)
          (newState, newState != cellState)
        } else if (neighbourCount == 3) {
          val newState = liveOrBorn()
          (newState, newState != cellState)
        } else {
          val newState = die(cellState)
          (newState, newState != cellState)
        }

      cellState = result._1
      replyTo ! CellStateUpdated(cellId, result._1, result._2)
      context.log.debug("Cell {} new state: {} because of neighbour count {}", cellId, cellState, neighbourCount)
      Behaviors.same
  }

}