package com.emotioncity.soriento

import com.emotioncity.soriento.ReflectionUtils._
import com.emotioncity.soriento.support.OrientDbSupport
import com.emotioncity.soriento.testmodels._
import com.orientechnologies.orient.core.id.ORecordId
import org.scalatest.{BeforeAndAfter, FunSuite, Matchers}
import org.scalatest.OptionValues._
import scala.reflect.runtime.universe._

/**
 * Created by stream on 25.12.14.
 */


class ReflectionTest extends FunSuite with Matchers with ODb with BeforeAndAfter {

  test("detect ORID in case class instance") {
    val complexWithRid = ComplexWithRid(id = ORecordId.EMPTY_RECORD_ID, 1, Simple("tt"), "tt", Nil)
    rid(complexWithRid) should not be empty
    val computedRid = rid(complexWithRid)
    computedRid.get should equal(ORecordId.EMPTY_RECORD_ID)
  }

  test("detect Option[ORID], ORID representation of @rid in case class instance") {
    //------- Option[ORID]
    val classWithOptionalRid = ClassWithOptionalRid(rid = Option(ORecordId.EMPTY_RECORD_ID), "name")
    rid(classWithOptionalRid) should not be empty
    val computedRid1 = rid(classWithOptionalRid)
    computedRid1.get should equal(ORecordId.EMPTY_RECORD_ID)

    val classWithOptionalRidNone = ClassWithOptionalRid(name = "name2")
    rid(classWithOptionalRidNone) shouldBe empty
    val computedRid2 = rid(classWithOptionalRidNone)
    computedRid2 should equal(None)

    //------ ORID
    val classWithRid = ClassWithRid(rid = ORecordId.EMPTY_RECORD_ID, "name")
    rid(classWithRid) should not be empty
    val computedRid3 = rid(classWithRid)
    computedRid3.get should equal(ORecordId.EMPTY_RECORD_ID)

    val classWitRidNull = ClassWithRid(name = "name2")
    rid(classWitRidNull) shouldBe empty
    val computedRid4 = rid(classWitRidNull)
    computedRid4 should equal(None)
  }

  test("should return parameter of type") {
    val clz = Class.forName("com.emotioncity.soriento.testmodels.Blah")
    val sFieldTpe = getScalaGenericTypeClass("sField", clz)
    sFieldTpe shouldBe None

    val dFieldTpe = getScalaGenericTypeClass("dField", clz)
    dFieldTpe shouldBe defined
    dFieldTpe.value shouldBe typeOf[Double]

    val bFieldTpe = getScalaGenericTypeClass("bField", clz)
    bFieldTpe shouldBe defined
    bFieldTpe.value shouldBe typeOf[Boolean]
  }

  after {
    dropOClass[Simple]
    dropOClass[Complex]
  }

}