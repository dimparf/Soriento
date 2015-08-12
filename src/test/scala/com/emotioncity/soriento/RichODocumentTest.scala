package com.emotioncity.soriento

import java.util

import com.emotioncity.soriento.RichODatabaseDocumentImpl._
import com.orientechnologies.orient.core.metadata.schema.OType
import com.orientechnologies.orient.core.record.impl.ODocument
import org.scalatest.{BeforeAndAfter, FunSuite, Matchers}

import scala.concurrent.Await
import scala.concurrent.duration._


/** ../
  * Created by stream on 31.03.15.
  */

class RichODocumentTest extends FunSuite with Matchers with BeforeAndAfter with Dsl with ODb {


  test("RichODocument should be provide implicit methods for read case class with @Embedded fields from ODocument") {
    createOClass[Home]
    val brothers: java.util.List[ODocument] = new util.ArrayList[ODocument]()
    brothers.add(new ODocument("Brother").field("name", "Blast").field("fooBar", "Morf"))
    brothers.add(new ODocument("Brother").field("name", "Faz")) //.field("fooBar", "Morf2"))
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

    val complexFromDb = complexList.head

    complexFromDb should equal(Complex(2, Simple("sField"), "sField", List(Simple("sFiesdfgdafgld"), Simple("asdfasdf"))))

  }

  test("RichODocument should retrieve object by id, and update it") {
    createOClass[ComplexWithRid]
    val simpleDoc = new ODocument("Simple").field("sField", "sField")
    val listField: util.List[ODocument] = new util.ArrayList[ODocument]()
    listField.add(new ODocument("Simple").field("sField", "sFiesdfgdafgld"))
    listField.add(new ODocument("Simple").field("sField", "asdfasdf"))
    val complexWithRidDoc = new ODocument("ComplexWithRid")
      .field("iField", 3)
      .field("simple", simpleDoc, OType.EMBEDDED)
      .field("listField", listField, OType.EMBEDDEDLIST)
    orientDb.save(complexWithRidDoc)

    val complexWithRid = ComplexWithRid(complexWithRidDoc.getIdentity, 3, Simple("sField"), "sField", List(Simple("sFiesdfgdafgld"), Simple("asdfasdf")))
    val updated = complexWithRid.copy(simple = Simple("New Value"))
    val updatedDoc = updated.save()
    updatedDoc.field[ODocument]("simple").field[String]("sField") should equal("New Value")

    val extractedObjects: List[ComplexWithRid] = orientDb.queryBySql[ComplexWithRid](s"select from ComplexWithRid where @rid = ${complexWithRidDoc.getIdentity}")

    extractedObjects.headOption should not be empty
    val extractedObject = extractedObjects.head
    extractedObject.id should equal(complexWithRidDoc.getIdentity)
  }

  test("select documents with @Linked document") {
    createOClass[BlogWithLinkedMessage]
    val blogWithLinkedMessage = BlogWithLinkedMessage(name = "Test", Message("FooBar"))
    blogWithLinkedMessage.save()
    val extractedBlogs = orientDb.queryBySql[BlogWithLinkedMessage]("select from BlogWithLinkedMessage where name = 'Test'")
    val extractedBlogOpt = extractedBlogs.headOption
    extractedBlogOpt should not be empty
    val extractedBlog = extractedBlogOpt.get
    extractedBlog.message should equal(Message("FooBar"))
  }

  test("select documents using async query") {
    createOClass[BlogWithLinkedMessage]
    val blogWithLinkedMessage = BlogWithLinkedMessage(name = "Test3", Message("FooBar"))
    blogWithLinkedMessage.save()
    val extractedBlogs = orientDb.asyncQueryBySql[BlogWithLinkedMessage]("select from BlogWithLinkedMessage where name = 'Test3'")

    val extractedBlogOpt = Await.result(extractedBlogs, 1 seconds).headOption
    extractedBlogOpt should not be empty
    val extractedBlog = extractedBlogOpt.get
    extractedBlog.message should equal(Message("FooBar"))
  }

  after {
    dropOClass[Home]
    dropOClass[Brother]
    dropOClass[Family]
    dropOClass[Simple]
    dropOClass[Complex]
    dropOClass[ComplexWithRid]
    dropOClass[BlogWithLinkedMessage]
  }

}
