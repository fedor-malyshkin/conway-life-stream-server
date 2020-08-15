package ru.fedor.conway.life.stream.server

import akka.actor.testkit.typed.scaladsl.{FishingOutcomes, ScalaTestWithActorTestKit}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import ru.fedor.conway.life.stream.server.Cell.{CellStateActive, CellStateDead}
import ru.fedor.conway.life.stream.server.FieldController.{FieldStateEvent, GameEnded, GameTurnEnded}

import scala.concurrent.duration.DurationInt


class FieldTest extends ScalaTestWithActorTestKit with AnyWordSpecLike with Matchers {

  val testField: Map[CellId, Cell.CellState] = Map.from(List(CellId(1, 1) -> CellStateActive))

  "Field actor" should {
    "calculate neighbours" in {
      /**
       * |x|_|_|
       * |_|x|_|
       * |x|x|_|
       * |_|_|_|
       * |_|_|_|
       */

      def cellId(fieldWidth: Int, fieldHeight: Int)(x: Int, y: Int) = CellId(fieldWidth, fieldHeight, x, y)

      def cellId35 = cellId(3, 5) _

      // create field

      val cellIds = FieldStateGenerator.unfold(cellId35(0, 0), List.empty)
      val states = List(CellStateActive, CellStateDead(0), CellStateDead(0),
        CellStateDead(0), CellStateActive, CellStateDead(0),
        CellStateActive, CellStateActive, CellStateDead(0),
        CellStateDead(0), CellStateDead(0), CellStateDead(0),
        CellStateDead(0), CellStateDead(0), CellStateDead(0))

      val field = Map.from(cellIds.zip(states))

      // check neighbour count
      Field.calculateNeighbourCount(cellId35(0, 0), field) shouldBe 1
      Field.calculateNeighbourCount(cellId35(1, 0), field) shouldBe 2
      Field.calculateNeighbourCount(cellId35(2, 0), field) shouldBe 1
      Field.calculateNeighbourCount(cellId35(0, 1), field) shouldBe 4
      Field.calculateNeighbourCount(cellId35(1, 4), field) shouldBe 0
    }

    "end game eventually" in {
      val testable = testKit.spawn(Field())
      val probe = testKit.createTestProbe[FieldController.FieldControllerMessage]()
      testable ! Field.GameStart(testField, probe.ref)
       probe.fishForMessage(1.second) {
        case FieldController.GameEnded =>
          FishingOutcomes.complete
        case _ =>
          FishingOutcomes.continueAndIgnore
      } should have size(1)

    }

    "push events" in {
      val testable = testKit.spawn(Field())
      val probe = testKit.createTestProbe[FieldController.FieldControllerMessage]()
      testable ! Field.GameStart(testField, probe.ref)
      val messages = probe.receiveMessages(21)
      messages should have size(21)
      messages(0) shouldBe FieldStateEvent(CellId(1, 1), CellStateDead(0))
      messages(1) shouldBe GameTurnEnded
      messages(18) shouldBe FieldStateEvent(CellId(1, 1), CellStateDead(9))
      messages(19) shouldBe GameTurnEnded
      messages(20) shouldBe GameEnded
    }

  }

}
