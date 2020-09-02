package ru.fedor.conway.life.stream.server

import akka.actor.typed
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes}
import akka.http.scaladsl.server.Directives
import akka.http.scaladsl.server.Directives.{complete, concat, handleWebSocketMessages, path}
import akka.http.scaladsl.{Http, model}
import akka.stream.scaladsl.{BroadcastHub, Flow, Keep, Sink, Source, SourceQueueWithComplete}
import akka.stream.{Materializer, OverflowStrategy}
import akka.util.ByteString
import akka.{Done, NotUsed}
import com.typesafe.config.{Config, ConfigFactory}
import org.slf4j.Logger
import ru.fedor.conway.life.stream.server.Cell.CellState
import ru.fedor.conway.life.stream.server.FieldController.FieldControllerMessage

import scala.List
import scala.collection.immutable
import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContextExecutor, Future}
import scala.language.postfixOps


trait HttpEndpoint {

  this: HttpEndpoint with ToJsonSerializer =>

  implicit def actorSystem: typed.ActorSystem[Nothing]

  implicit def materializer: Materializer

  implicit val executionContext: ExecutionContextExecutor = actorSystem.executionContext
  private var healthState = false
  private val conf: Config = ConfigFactory.load()
  private val BIND_HOST = conf.getString(s"${Server.CONF_ROOT}.bind-host")
  private val BIND_PORT = conf.getInt(s"${Server.CONF_ROOT}.bind-port")

  def overflowStrategy: OverflowStrategy = OverflowStrategy.dropHead

  private def streamSource(): Source[String, NotUsed] = {
    Source.single(mapToJson(currentSnapshot())).
      concat(fromProducer).
      buffer(100, overflowStrategy) // we don't want to suffer because of "slow" clients
  }

  private def httpStream: Source[ByteString, NotUsed] = {
    streamSource().
      wireTap(msg => log().trace(s"Sent to HTTP client: $msg")).
      map(s => ByteString(s"$s\n"))
  }

  private def wsStream: Flow[Message, Message, Any] = {
    val textMessageSteam = streamSource().
      wireTap(msg => log().trace(s"Sent to WS client: $msg")).
      map(TextMessage(_))
    Flow.fromSinkAndSourceCoupled(Sink.ignore, textMessageSteam)
  }

  private val route =
    concat(
      path("health") {
        Directives.get {
          if (healthState)
            complete(StatusCodes.OK, model.HttpEntity("<h1>Everything is fine :)</h1>"))
          else
            complete(StatusCodes.InternalServerError, model.HttpEntity("<h1>We have some problem :(</h1>"))
        }
      },
      path("ws") {
        handleWebSocketMessages(wsStream)
      },
      path("stream") {
        Directives.get {
          complete(HttpEntity(ContentTypes.`text/plain(UTF-8)`, httpStream))
        }
      })

  private val bindingFuture = Http().newServerAt(BIND_HOST, BIND_PORT).bindFlow(route)

  private val queueSource = Source.queue[FieldControllerMessage](10240, overflowStrategy).
    groupedWithin(100, 500 milli). // we don't want overload clients by messages
    mapConcat(sq => groupInBatches(sq)) //... so we group them into composite message

  private val runnableGraph = queueSource.toMat(BroadcastHub.sink(bufferSize = 256))(Keep.both)

  private val (queue, fromProducer) = runnableGraph.run()
  // single producer to avoid overflow of buffer internal buffer
  fromProducer.runWith(Sink.ignore)

  def downstreamQueue(): SourceQueueWithComplete[FieldControllerMessage] = queue

  def healthy(state: Boolean): Unit = {
    healthState = state
  }

  def unbind(): Future[Done] = Await.ready(bindingFuture.flatMap(_.unbind()), 2.second)

  def currentSnapshot(): Map[CellId, CellState]

  def log(): Logger

  def groupInBatches(sq: Seq[FieldControllerMessage]): immutable.Iterable[String]
}

