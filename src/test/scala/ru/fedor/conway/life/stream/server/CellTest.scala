package ru.fedor.conway.life.stream.server

import akka.actor.testkit.typed.scaladsl.{BehaviorTestKit, TestInbox}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import ru.fedor.conway.life.stream.server.Cell.{CellStateActive, CellStateDead}
import ru.fedor.conway.life.stream.server.Field.CellStateUpdated

class CellTest extends AnyFlatSpec with Matchers {

  val cellId = CellId(1, 1)

  it should "die due underpopulation" in {
    val testable = BehaviorTestKit(Cell(cellId, CellStateActive))

    val inbox = TestInbox[Field.FieldMessage]()

    testable.run(Cell.CellStateUpdate(1, inbox.ref))
    inbox.expectMessage(CellStateUpdated(cellId, CellStateDead(0), true))
    testable.run(Cell.CellStateUpdate(1, inbox.ref))
    inbox.expectMessage(CellStateUpdated(cellId, CellStateDead(1), true))
  }

  it should "born in case of 3" in {
    val testable = BehaviorTestKit(Cell(cellId, CellStateDead(3)))

    val inbox = TestInbox[Field.FieldMessage]()

    testable.run(Cell.CellStateUpdate(3, inbox.ref))
    inbox.expectMessage(CellStateUpdated(cellId, CellStateActive, true))
    testable.run(Cell.CellStateUpdate(3, inbox.ref))
    inbox.expectMessage(CellStateUpdated(cellId, CellStateActive, false))
  }

  it should "live in case of 2 or 3" in {
    val testable = BehaviorTestKit(Cell(cellId, CellStateActive))

    val inbox = TestInbox[Field.FieldMessage]()

    testable.run(Cell.CellStateUpdate(2, inbox.ref))
    inbox.expectMessage(CellStateUpdated(cellId, CellStateActive, false))
    testable.run(Cell.CellStateUpdate(3, inbox.ref))
    inbox.expectMessage(CellStateUpdated(cellId, CellStateActive, false))
  }

  it should "die because of overpopulation" in {
    val testable = BehaviorTestKit(Cell(cellId, CellStateActive))

    val inbox = TestInbox[Field.FieldMessage]()

    testable.run(Cell.CellStateUpdate(4, inbox.ref))
    inbox.expectMessage(CellStateUpdated(cellId, CellStateDead(0), true))
    testable.run(Cell.CellStateUpdate(6, inbox.ref))
    inbox.expectMessage(CellStateUpdated(cellId, CellStateDead(1), true))
  }

}