package ru.fedor.conway.life.stream.server

import akka.actor.typed.ActorSystem


object Server {
  val SERVER_NAME = "conway-life-stream-server"

  def main(args: Array[String]): Unit = {
    ActorSystem[Nothing](FieldSupervisor(), SERVER_NAME)
  }

}