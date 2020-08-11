package ru.fedor.conway.life.stream.server

import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior, PostStop, Signal}
import com.typesafe.config.{Config, ConfigFactory}
import ru.fedor.conway.life.stream.server.Cell.{CellStateActive, CellStateDead}
import ru.fedor.conway.life.stream.server.FieldController.{FieldControllerMessage, FieldStateGenerated}
import ru.fedor.conway.life.stream.server.FieldStateGenerator.FieldStateMessage

import scala.annotation.tailrec
import scala.util.Random


object FieldStateGenerator {

  sealed trait FieldStateMessage

  case class FieldStateGenerate(replyTo: ActorRef[FieldControllerMessage]) extends FieldStateMessage

  def apply(): Behavior[FieldStateMessage] =
    Behaviors.setup[FieldStateMessage](context => new FieldStateGenerator(context))

  val conf: Config = ConfigFactory.load()
  private val FIELD_WIDTH = conf.getInt(s"${Server.CONF_ROOT}.game.field-width")
  private val FIELD_HEIGHT = conf.getInt(s"${Server.CONF_ROOT}.game.field-height")
  private val FIELD_INITIAL_SEED_COUNT = conf.getInt(s"${Server.CONF_ROOT}.game.field-initial-seed-count")

  @tailrec
  def unfold(cellId: CellId, acc: List[CellId]): List[CellId] = {
    cellId.nextX match {
      case Some(value) => unfold(value, acc :+ cellId)
      case None => cellId.startNextY match {
        case Some(value) => unfold(value, acc :+ cellId)
        case None => acc :+ cellId
      }
    }
  }
}

class FieldStateGenerator(context: ActorContext[FieldStateMessage]) extends AbstractBehavior[FieldStateMessage](context) {

  import ru.fedor.conway.life.stream.server.FieldStateGenerator._

  override def onMessage(msg: FieldStateMessage): Behavior[FieldStateMessage] =
    msg match {
      case FieldStateGenerator.FieldStateGenerate(replyTo) =>
        replyTo ! FieldStateGenerated(generateField())
        Behaviors.same
    }

  override def onSignal: PartialFunction[Signal, Behavior[FieldStateMessage]] = {
    case PostStop =>
      this
  }

  def generateField(): Map[CellId, Cell.CellState] = {


    val cellIds = unfold(CellId(FIELD_WIDTH, FIELD_HEIGHT), List.empty)

    // random indexes
    val randomIndexes = Range.apply(0, FIELD_INITIAL_SEED_COUNT).
      map(_ => Random.between(0, FIELD_WIDTH * FIELD_HEIGHT)).
      toSet

    val cellValues = Range.apply(0, FIELD_WIDTH * FIELD_HEIGHT).
      map { ndx => if (randomIndexes.contains(ndx)) CellStateActive else CellStateDead(0) }

    Map.from(cellIds.zip(cellValues))
  }
}
