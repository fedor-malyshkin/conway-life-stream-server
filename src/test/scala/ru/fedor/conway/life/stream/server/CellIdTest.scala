package ru.fedor.conway.life.stream.server

import org.scalatest.OptionValues
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class CellIdTest extends AnyFlatSpec with Matchers with OptionValues {

  "An empty CellId" should "not have next or prev" in {
    CellId(0, 0).nextX shouldBe None
    CellId(0, 0).prevX shouldBe None
  }

  "An usual CellId" should "working next/prev" in {
    CellId(2, 2).nextX.value shouldBe CellId(2, 2, 1, 0)
    CellId(2, 2, 1, 0).prevX.value shouldBe CellId(2, 2, 0, 0)
  }
}