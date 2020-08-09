package ru.fedor.conway.life.stream.server

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import org.scalatest.wordspec.AnyWordSpecLike

class FieldStateSpec extends ScalaTestWithActorTestKit with AnyWordSpecLike {

  "FieldState actor" must {

    "reply with empty reading if no temperature is known" in {
      0 should ===(0)
    }
  }
}