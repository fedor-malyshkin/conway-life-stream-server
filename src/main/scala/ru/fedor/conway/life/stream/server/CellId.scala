package ru.fedor.conway.life.stream.server

object CellId {
  def apply(fieldWidth: Int, fieldHeight: Int) = new CellId(fieldWidth, fieldHeight, 0, 0)

  def apply(fieldWidth: Int,
            fieldHeight: Int,
            x: Int,
            y: Int) = new CellId(fieldWidth, fieldHeight, x, y)
}

/**
 * Field coordinate.
 *
 * <ul>
 * <li>x: [0..fieldWidth)</li>
 * <li> y: [0..fieldHeight)</li>
 * </ul>
 *
 */
case class CellId(fieldWidth: Int,
                  fieldHeight: Int,
                  x: Int,
                  y: Int) {

  def nextX: Option[CellId] =
    if (x + 1 < fieldWidth)
      Some(CellId(fieldWidth, fieldHeight, x + 1, y))
    else
      None

  def prevX: Option[CellId] =
    if (x - 1 >= 0)
      Some(CellId(fieldWidth, fieldHeight, x - 1, y))
    else
      None

  def nextY: Option[CellId] =
    if (y + 1 < fieldHeight)
      Some(CellId(fieldWidth, fieldHeight, x, y + 1))
    else
      None

  def startNextY: Option[CellId] =
    if (y + 1 < fieldHeight)
      Some(CellId(fieldWidth, fieldHeight, 0, y + 1))
    else
      None

  def prevY: Option[CellId] =
    if (y - 1 >= 0)
      Some(CellId(fieldWidth, fieldHeight, x, y - 1))
    else
      None
}
