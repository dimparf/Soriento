package com.emotioncity.soriento

import java.util

import com.orientechnologies.orient.core.metadata.schema.OType
import com.orientechnologies.orient.core.record.impl.ODocument
import org.scalatest.{BeforeAndAfter, FunSuite, Matchers}
import RichODatabaseDocumentImpl._


/**../
 * Created by stream on 31.03.15.
 */

class RichODocumentTest extends FunSuite with Matchers with BeforeAndAfter with Dsl with OrientDbSupport with ODb {


  test("RichODocument should be provide implicit methods for read case class with @Embedded fields from ODocument") {
    createOClass[Home]
    val brothers: java.util.List[ODocument] = new util.ArrayList[ODocument]()
    brothers.add(new ODocument("Brother").field("name", "Blast").field("fooBar", "Morf"))
    brothers.add(new ODocument("Brother").field("name", "Faz"))//.field("fooBar", "Morf2"))
    val oDocument = new ODocument("Home")
      .field("name", "Sweet home")
      .field("family",
        new ODocument("Family")
          .field("mother", "Tata")
          .field("father", "Rembo")
          .field("brothers", brothers, OType.EMBEDDEDLIST
          ), OType.EMBEDDED)
    orientDb.save(oDocument)

    val homeList = orientDb.queryBySql[Home]("select from Home")

    homeList.head should equal(Home("Sweet home", Family("Tata", "Rembo", List(Brother("Blast", Some("Morf")), Brother("Faz", None)))))
    createOClass[Complex]
    createOClass[Simple]
    val simpleDoc = new ODocument("Simple").field("sField", "sField")
    val listField: util.List[ODocument] = new util.ArrayList[ODocument]()
    listField.add(new ODocument("Simple").field("sField", "sFiesdfgdafgld"))
    listField.add(new ODocument("Simple").field("sField", "asdfasdf"))

    val complex = new ODocument("Complex")
      .field("iField", 2)
      .field("simple", simpleDoc, OType.EMBEDDED)
      .field("sField", "sField")
      .field("listField", listField, OType.EMBEDDEDLIST)
    orientDb.save(complex)

    val complexList = orientDb.queryBySql[Complex]("select from Complex")

    complexList.head should equal(Complex(2, Simple("sField"), "sField", List(Simple("sFiesdfgdafgld"), Simple("asdfasdf"))))
  }


  after {
    /*dropOClass[Home]
    dropOClass[Brother]
    dropOClass[Family]*/
    dropOClass[Simple]
    dropOClass[Complex]
  }

}
