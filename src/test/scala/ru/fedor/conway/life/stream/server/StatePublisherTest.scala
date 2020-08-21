package ru.fedor.conway.life.stream.server

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import ru.fedor.conway.life.stream.server.Cell.{CellStateActive, CellStateDead}
import ru.fedor.conway.life.stream.server.FieldController.{FieldStateEvent, GameEnded, GameStart}

class StatePublisherTest extends AnyFlatSpec with Matchers {
  it should "group in batches empty list" in {
    StatePublisher.groupInBatchesInt(List.empty) should have size (0)
  }

  it should "group in batches" in {
    StatePublisher.groupInBatchesInt(List(GameEnded,
      GameStart,
      GameStart,
      GameEnded,
      FieldStateEvent(CellId(0, 0), CellStateActive), FieldStateEvent(CellId(1, 2), CellStateDead(1)))) should contain theSameElementsAs
      Vector(List(GameEnded), List(GameStart, GameStart),
        List(GameEnded),
        List(FieldStateEvent(CellId(0, 0), CellStateActive), FieldStateEvent(CellId(1, 2), CellStateDead(1))))
  }
}
