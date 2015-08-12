package com.emotioncity.soriento

import com.emotioncity.soriento.ReflectionUtils._
import org.scalatest.{BeforeAndAfter, FunSuite, Matchers}

/**
 * Created by stream on 25.12.14.
 */


class ReflectionTest extends FunSuite with Matchers with ODb with BeforeAndAfter {

  test("it should be create instance of case class by name simple and recursively") {
    val simpleMap = Map("sField" -> "Test field")
    val simpleCaseClass = createCaseClass[Simple](simpleMap)
    simpleCaseClass should equal(Simple("Test field"))

    val complexMap = Map("iField" -> 2, "sField" -> "tt", "simple" -> Map("sField" -> "Simple"), "listField" -> List(Simple("Simple")))
    val complexCaseClass = createCaseClass[Complex](complexMap)
    val simple = Simple("Simple")
    complexCaseClass should equal(Complex(2, simple, sField = "tt", List(simple)))
  }

  after {
    dropOClass[Simple]
    dropOClass[Complex]
  }

}
