package ru.fedor.conway.life.stream.server

import akka.actor.typed
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior, PostStop, Signal}
import akka.http.scaladsl.model.ws.TextMessage
import akka.stream.{Materializer, QueueOfferResult}
import ru.fedor.conway.life.stream.server.Cell.CellState
import ru.fedor.conway.life.stream.server.FieldController.{FieldControllerMessage, SubscriberAdd}
import ru.fedor.conway.life.stream.server.StatePublisher.StatePublisherMessage

import scala.collection.immutable
import scala.concurrent.Future


object StatePublisher {

  sealed trait StatePublisherMessage

  case class WrapperStatePublisherMessage(msg: FieldControllerMessage) extends StatePublisherMessage

  case class FieldControllerSubscribe(producer: ActorRef[FieldControllerMessage]) extends StatePublisherMessage

  def apply(): Behavior[StatePublisherMessage] =
    Behaviors.setup[StatePublisherMessage] { context =>
      new StatePublisher(context)
    }

  private def aggregateState(msg: FieldControllerMessage, currentState: Map[CellId, CellState]): Map[CellId, CellState] =
    msg match {
      case FieldController.GameEnded =>
        Map.empty
      case FieldController.GameStart =>
        Map.empty
      case FieldController.GameTurnEnded =>
        currentState
      case FieldController.FieldStateEvent(cellId, cellState, _) =>
        currentState + (cellId -> cellState)
      case _ =>
        currentState
    }


  def groupInBatchesInt(sq: Seq[FieldControllerMessage]): Seq[List[FieldControllerMessage]] = {
    case class Collector(last: Class[_ <: FieldControllerMessage],
                         current: List[FieldControllerMessage],
                         collected: List[List[FieldControllerMessage]])
    if (sq.isEmpty) List.empty
    else {
      val agg = Collector(sq.head.getClass, List.empty, List.empty)
      val aggregated = sq.foldLeft(agg) {
        case (agg, el) =>
          if (el.getClass == agg.last)
            Collector(el.getClass, agg.current :+ el, agg.collected)
          else
            Collector(el.getClass, List(el), agg.collected :+ agg.current)
      }

      (aggregated.collected :+ aggregated.current).filterNot(_.isEmpty)
    }
  }
}

class StatePublisher(context: ActorContext[StatePublisherMessage]) extends AbstractBehavior[StatePublisherMessage](context)
  with ToJsonSerializer with HttpEndpoint {

  import ru.fedor.conway.life.stream.server.StatePublisher._

  var currentState: Map[CellId, CellState] = Map.empty

  def actorSystem: typed.ActorSystem[Nothing] = context.system

  implicit def materializer: Materializer = Materializer(actorSystem)

  def pushMessageToStream(msg: FieldControllerMessage): Future[Unit] =
    downstreamQueue().offer(msg).map {
      case msgMatch@(QueueOfferResult.Dropped |
                     QueueOfferResult.Failure(_) |
                     QueueOfferResult.QueueClosed) =>
        context.log.error(s"$msgMatch")
      case _ =>
    }

  val fieldControllerAdapter: ActorRef[FieldControllerMessage] =
    context.messageAdapter(rsp => WrapperStatePublisherMessage(rsp))

  override def onMessage(msg: StatePublisherMessage): Behavior[StatePublisherMessage] =
    msg match {
      case WrapperStatePublisherMessage(msg) =>
        // context.log.debug(s"Got event: $msg")
        healthy(true) // the first notification could be considered as an successful start
        msg match {
          case FieldController.GameEnded |
               FieldController.GameStart |
               FieldController.GameTurnEnded |
               FieldController.FieldStateEvent(_, _, _) =>
            currentState = aggregateState(msg, currentState)
            pushMessageToStream(msg)
          case _ =>
        }
        Behaviors.same
      case StatePublisher.FieldControllerSubscribe(producer) =>
        producer ! SubscriberAdd(fieldControllerAdapter)
        Behaviors.same
    }

  override def onSignal: PartialFunction[Signal, Behavior[StatePublisherMessage]] = {
    case PostStop =>
      unbind()
      this
  }

  /**
   * Group in batches keeping existing order and convert batches to TextMessage.
   */
  def groupInBatches(sq: Seq[FieldControllerMessage]): immutable.Iterable[TextMessage] = {
    groupInBatchesInt(sq).
      map(listToJson).
      map(TextMessage(_))
  }
}