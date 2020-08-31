package ru.fedor.conway.life.stream.server

import java.util.concurrent.TimeUnit

import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors, TimerScheduler}
import akka.actor.typed.{ActorRef, Behavior}
import com.typesafe.config.{Config, ConfigFactory}
import ru.fedor.conway.life.stream.server.Cell.{CellMessage, CellState, CellStateActive, CellStateDead}
import ru.fedor.conway.life.stream.server.Field.FieldMessage
import ru.fedor.conway.life.stream.server.FieldController.FieldControllerMessage

import scala.collection.mutable
import scala.concurrent.duration.FiniteDuration

object Field {

  sealed trait FieldMessage

  case class GameStart(fieldState: Map[CellId, CellState], replyTo: ActorRef[FieldControllerMessage]) extends FieldMessage

  case class CellStateUpdated(cellId: CellId, cellState: CellState, hasChanges: Boolean) extends FieldMessage

  object GameTurnStart extends FieldMessage

  def apply(): Behavior[FieldMessage] =
    Behaviors.setup[FieldMessage] { context =>
      Behaviors.withTimers { timers =>
        new Field(context, timers, None, mutable.AnyRefMap.empty, Map.empty)
      }
    }

  def calculateNeighbourCount(cellId: CellId, fieldMap: collection.Map[CellId, CellState]): Int = {
    def countCell(cellIdValue: CellId, fieldMap: collection.Map[CellId, CellState]) =
      if (fieldMap.getOrElse(cellIdValue, CellStateDead(0)) == CellStateActive) 1 else 0

    def calculateNeighbourCount(cellId: Option[CellId], fieldMap: collection.Map[CellId, CellState], extendRow: Boolean): Int = {
      cellId match {
        case Some(cellIdValue) =>
          if (extendRow) {
            calculateNeighbourCount(cellIdValue.prevX, fieldMap, extendRow = false) +
              calculateNeighbourCount(cellIdValue.nextX, fieldMap, extendRow = false) +
              countCell(cellIdValue, fieldMap)
          }
          else
            countCell(cellIdValue, fieldMap)
        case None =>
          0
      }
    }

    // up 3 cell
    calculateNeighbourCount(cellId.prevY, fieldMap, extendRow = true) +
      // left cell
      calculateNeighbourCount(cellId.prevX, fieldMap, extendRow = false) +
      // right cell
      calculateNeighbourCount(cellId.nextX, fieldMap, extendRow = false) +
      // down 3 cell
      calculateNeighbourCount(cellId.nextY, fieldMap, extendRow = true)
  }

  def amountOfLive(fieldMap: collection.Map[CellId, CellState]): Int = fieldMap.values.count(CellStateActive == _)

  val conf: Config = ConfigFactory.load()
  private val GAME_TURN_DURATION = FiniteDuration.apply(
    conf.getDuration(s"${Server.CONF_ROOT}.game.turn-length", TimeUnit.MILLISECONDS), TimeUnit.MILLISECONDS
  )
  private val GAME_TURN_LIMIT = conf.getInt(s"${Server.CONF_ROOT}.game.turn-limit")
}

class Field(context: ActorContext[FieldMessage],
            timers: TimerScheduler[FieldMessage],
            supervisor: Option[ActorRef[FieldControllerMessage]],
            fieldMap: mutable.AnyRefMap[CellId, CellState],
            cellsMap: Map[CellId, ActorRef[CellMessage]]) extends AbstractBehavior[FieldMessage](context) {

  import ru.fedor.conway.life.stream.server.Field._

  var unansweredCells: Set[CellId] = Set.empty

  var hasChangesWithinTurn: Boolean = _

  var turnCount = 0

  override def onMessage(msg: FieldMessage): Behavior[FieldMessage] =
    msg match {
      case Field.GameStart(fieldState, replyTo) =>
        context.log.info("Game started")
        startTimer()
        new Field(context, timers, Some(replyTo), mutable.AnyRefMap.from(fieldState), spawnNewCells(fieldState))
      case Field.GameTurnStart =>
        startGameTurn()
        Behaviors.same
      case res: Field.CellStateUpdated =>
        processCellResponse(res)
    }

  def spawnNewCells(fieldState: Map[CellId, CellState]): Map[CellId, ActorRef[CellMessage]] = {
    fieldState.map {
      case (cellId, cellState) =>
        (cellId, context.spawn(Cell(cellId, cellState), s"${cellId.x}_${cellId.y}"))
    }
  }


  def startGameTurn(): Unit = {
    if (unansweredCells.isEmpty) { // only in case if processed every cell by this time
      context.log.debug("The amount of live at the beginning of the turn: {}", amountOfLive(fieldMap))

      turnCount += 1
      if (turnCount < GAME_TURN_LIMIT) processGameTurn() else stopGame()
    }
  }

  def processGameTurn(): Unit = {
    hasChangesWithinTurn = false
    unansweredCells = Set.from(cellsMap.keys)
    // make snapshot - as could be changed during calling all cells
    val staticMap = Map.from(fieldMap)
    cellsMap.foreach {
      case (cellId, ref) => ref ! Cell.CellStateUpdate(calculateNeighbourCount(cellId, staticMap), context.self)
    }
  }

  def processCellResponse(response: Field.CellStateUpdated): Behavior[FieldMessage] = {
    if (response.hasChanges)
      supervisor.foreach(_ ! FieldController.FieldStateEvent(response.cellId, response.cellState, turnCount, turnsLeft))
    unansweredCells = unansweredCells - response.cellId
    updateField(response.cellId, response.cellState)
    hasChangesWithinTurn = hasChangesWithinTurn || response.hasChanges
    if (unansweredCells.isEmpty) {

      if (!hasChangesWithinTurn) {
        stopGame()
      } else {
        supervisor.foreach(_ ! FieldController.GameTurnEnded)
        context.log.debug("The amount of live at the end of the turn: {} (left: {})", amountOfLive(fieldMap), turnsLeft)
        context.log.trace("Internal state at the end: {}", fieldMap)
        Behaviors.same
      }
    } else Behaviors.same
  }

  def turnsLeft: Int = GAME_TURN_LIMIT - turnCount

  def startTimer(): Unit = timers.startTimerAtFixedRate(Field.GameTurnStart, GAME_TURN_DURATION)

  def stopTimer(): Unit = timers.cancel(Field.GameTurnStart)

  def updateField(cellId: CellId, cellState: CellState): Option[CellState] = fieldMap.put(cellId, cellState)

  def stopGame(): Behavior[Field.FieldMessage] = {
    stopTimer()
    supervisor.foreach(_ ! FieldController.GameEnded)
    Behaviors.stopped
  }


}