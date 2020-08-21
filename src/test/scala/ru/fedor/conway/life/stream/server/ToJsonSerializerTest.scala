package ru.fedor.conway.life.stream.server

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import ru.fedor.conway.life.stream.server.Cell.{CellStateActive, CellStateDead}

class ToJsonSerializerTest extends AnyFlatSpec with Matchers with ToJsonSerializer {
  it should "serialize cells" in {
    import CellIdJsonProtocol._
    import spray.json._

    val json = CellId(158, 160).toJson
    json.compactPrint shouldBe """{"x":0,"y":0}"""
  }

  it should "serialize state" in {
    import CellStateJsonProtocol._
    import spray.json._

    val stateDead = CellStateDead(12)
    stateDead.toJson.compactPrint shouldBe """{"how-long":12,"state":"dead"}"""

    val stateActive = CellStateActive
    stateActive.toJson.compactPrint shouldBe """{"state":"active"}"""
  }

  it should "serialize snapshot" in {
    mapToJson(Map.empty) shouldBe """{"data":[],"height":0,"type":"snapshot","width":0}"""

    def cellId(fieldWidth: Int, fieldHeight: Int)(x: Int, y: Int) = CellId(fieldWidth, fieldHeight, x, y)

    def cellId12 = cellId(1, 2) _

    val snapshot = Map.from(List((cellId12(0, 0) -> CellStateActive),
      (cellId12(0, 1) -> CellStateDead(12))))
    mapToJson(snapshot) shouldBe """{"data":[{"state":"active","x":0,"y":0},{"how-long":12,"state":"dead","x":0,"y":1}],"height":2,"type":"snapshot","width":1}"""
  }

}
