package ru.fedor.conway.life.stream.server

import akka.actor.typed.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.server.Directives
import akka.http.scaladsl.server.Directives.{complete, path}
import akka.stream.ActorMaterializer


object Server {
  val SERVER_NAME = "conway-life-stream-server"

  def main(args: Array[String]): Unit = {

    val typedActorSystem = ActorSystem[Nothing](FieldSupervisor(), SERVER_NAME)

    implicit val untypedSystem = typedActorSystem.classicSystem

    implicit val materializer = ActorMaterializer()
    // needed for the future flatMap/onComplete in the end
    implicit val executionContext = untypedSystem.dispatcher

    val route =
      path("") {
        Directives.get {
          complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, "<h1>Say hello to akka-http</h1>"))
        }
      }

    val bindingFuture = Http().newServerAt("0.0.0.0", 8080).bindFlow(route)
  }

}