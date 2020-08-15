package ru.fedor.conway.life.stream.server

import ru.fedor.conway.life.stream.server.Cell.{CellState, CellStateActive, CellStateDead}
import ru.fedor.conway.life.stream.server.FieldController.{FieldControllerMessage, FieldStateEvent}
import spray.json.{DefaultJsonProtocol, JsArray, JsNumber, JsObject, JsString, JsValue, RootJsonFormat} // if you don't supply your own Protocol (see below)

object CellIdJsonProtocol extends DefaultJsonProtocol {

  implicit object CellIdJsonFormat extends RootJsonFormat[CellId] {
    override def write(c: CellId) = JsObject(
      "x" -> JsNumber(c.x),
      "y" -> JsNumber(c.y)
    )

    override def read(json: JsValue): CellId = ???
  }

}

object CellStateJsonProtocol extends DefaultJsonProtocol {

  implicit object CellStateActiveJsonFormat extends RootJsonFormat[CellStateActive.type] {

    override def write(obj: Cell.CellStateActive.type): JsValue =
      JsObject(
        "state" -> JsString("active")
      )

    override def read(json: JsValue): CellStateActive.type = ???
  }

  implicit object CellStateDeadJsonFormat extends RootJsonFormat[CellStateDead] {
    override def write(c: CellStateDead): JsValue =
      JsObject(
        "state" -> JsString("dead"),
        "how-long" -> JsNumber(c.howLong)
      )

    override def read(json: JsValue): CellStateDead = ???
  }

}


trait ToJsonSerializer {


  def tupleToJsValue(t: (CellId, CellState)): JsValue = {
    val x = "x" -> JsNumber(t._1.x)
    val y = "y" -> JsNumber(t._1.y)
    t._2 match {
      case Cell.CellStateActive =>
        JsObject(
          x, y,
          "state" -> JsString("active")
        )
      case CellStateDead(howLong) =>
        JsObject(
          x, y,
          "state" -> JsString("dead"),
          "how-long" -> JsNumber(howLong)
        )
    }
  }

  private def mapToJson(fieldWidth: Int, fieldHeight: Int, values: Iterable[(CellId, CellState)]): String = {
    JsObject(
      "type" -> JsString("snapshot"),
      "width" -> JsNumber(fieldWidth),
      "height" -> JsNumber(fieldHeight),
      "data" -> JsArray(values.map(tupleToJsValue).toVector)
    ).compactPrint
  }

  def mapToJson(state: Map[CellId, CellState]): String = {
    state.size match {
      case 0 =>
        mapToJson(0, 0, state)
      case _ =>
        val head = state.keys.head
        mapToJson(head.fieldWidth, head.fieldHeight, state)
    }
  }


  def listToJson(l: List[FieldControllerMessage]): String = l.head match {
    case FieldController.GameEnded =>
      JsObject("type" -> JsString("game-ended")).compactPrint
    case FieldController.GameStart =>
      JsObject("type" -> JsString("game-start")).compactPrint
    case FieldController.GameTurnEnded =>
      JsObject("type" -> JsString("game-turn-ended")).compactPrint
    case FieldController.FieldStateEvent(_, _, stepLeft) =>
      JsObject("type" -> JsString("field-event"),
        "steps-left" -> JsNumber(stepLeft),
        "data" -> JsArray(l.map(eventToJson).toVector)).compactPrint
    case _ =>
      throw new IllegalStateException()
  }

  def eventToJson(event: FieldControllerMessage): JsValue = event match {
    case FieldStateEvent(cellId, cellState, stepsLeft) => tupleToJsValue((cellId, cellState))
    case _ =>
      throw new IllegalStateException()
  }

}
