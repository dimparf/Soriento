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


  test("RichODocument should be provide implicit methods for read case class with @Emedded fields from ODocument") {
    val brothers: java.util.List[ODocument] = new util.ArrayList[ODocument]()
    brothers.add(new ODocument("Brother").field("name", "Blast").field("kulugda", "Morf"))
    brothers.add(new ODocument("Brother").field("name", "Faz").field("kulugda", "Morf2"))
    val oDocument = new ODocument("Home")
      .field("name", "Tost")
      .field("family",
        new ODocument("Family")
          .field("mother", "Tata")
          .field("father", "Rembo")
          .field("brothers", brothers, OType.EMBEDDEDLIST
          ), OType.EMBEDDED)
    orientDb.save(oDocument)

    val blagdaList = orientDb.queryBySql[Home]("select from Home")

    blagdaList.head should equal(Home("Tost", Family("Tata", "Rembo", List(Brother("Blast", Some("Morf")), Brother("Faz", None)))))
  }

  test("RichODocument should be determinate type of field and map it to case class field") {
    val simple = Simple("stringField")
    val complex = Complex(2, simple, "string", List(simple))

    val simpleODoc = new ODocument("Simple").field("sField", "stringField", OType.STRING).save()

    //println("SimpleODoc sField Type: " + simpleODoc.fieldType("sField"))

    val dbSimple = orientDb.queryBySql[Simple]("select from Simple").head
    //println(s"Simple from DB: $dbSimple")

    val simples: java.util.List[ODocument] = new util.ArrayList[ODocument]()
    simples.add(new ODocument("Simple").field("sField", "stringField", OType.STRING))
    val oDocument = new ODocument("Complex")
    .field("iField", 2)
    .field("simple", simple)
    .field("sField", "string")
    .field("listField", simples)
    orientDb.save(oDocument)
    val dbComplex = orientDb.queryBySql[Complex]("select from Complex").head
    //println(dbComplex)
  }


  after {
    dropOClass[Home]
    dropOClass[Brother]
    dropOClass[Family]
    dropOClass[Simple]
    dropOClass[Complex]
  }

}
