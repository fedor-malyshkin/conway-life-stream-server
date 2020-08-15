package ru.fedor.conway.life.stream.server

import akka.actor.typed.ActorSystem
import ru.fedor.conway.life.stream.server.ServerController.ServerControllerMessage


object Server {
  val SERVER_NAME = "conway-life-stream-server"
  val CONF_ROOT = "conway-life-stream-server"

  def main(args: Array[String]): Unit = {
    ActorSystem[ServerControllerMessage](ServerController(), SERVER_NAME)
  }

}