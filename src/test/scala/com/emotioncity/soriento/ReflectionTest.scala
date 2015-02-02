package com.emotioncity.soriento

import org.scalatest.{Matchers, FunSuite}
import com.emotioncity.soriento.ReflectionUtils._

/**
 * Created by stream on 25.12.14.
 */


class ReflectionTest extends FunSuite with Matchers {

  test("it should be create instance of case class by name simple and recursively") {
    val simpleMap = Map("sField" -> "Test field")
    val simpleCaseClass = createCaseClass[Simple](simpleMap)
    simpleCaseClass should be equals Simple("Test field")

    val complexMap = Map("iField" -> 2, "simple" -> Map("sField" -> "Simple"))
    val complexCaseClass = createCaseClass[Complex](complexMap)
    complexCaseClass should be equals Complex(2, Simple("Simple"))
  }

}
