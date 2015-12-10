package com.emotioncity.soriento

import java.util

import com.emotioncity.soriento.RichODatabaseDocumentImpl._
import com.emotioncity.soriento.RichODocumentImpl._
import com.emotioncity.soriento.config.SorientoConfig
import com.emotioncity.soriento.support.OrientDbSupport
import com.emotioncity.soriento.testmodels._
import com.orientechnologies.orient.core.metadata.schema.OType
import com.orientechnologies.orient.core.record.impl.ODocument
import org.scalatest.{BeforeAndAfter, FunSuite, Inside, Matchers}


/** ../
  * Created by stream on 31.03.15.
  */

class RichODocumentTest extends FunSuite with Matchers with BeforeAndAfter with Inside with Dsl
with ODb {

  import ODocumentReader._

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
    complexWithRidDoc.save()

    val complexWithRid = ComplexWithRid(complexWithRidDoc.getIdentity, 3, Simple("sField"), "s2Field", List(Simple("sFiesdfgdafgld"), Simple("asdfasdf")))
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

  test("select document with @LinkedSet documents") {
    createOClass[BlogWithLinkSetMessages]

    val messageOne = LinkedMessage("This is my first message")
    val messageOneSaved = messageOne.save.as[LinkedMessage].get
    val messageOneId = messageOneSaved.id

    val messageTwo = LinkedMessage("last")
    val messageTwoSaved = messageTwo.save.as[LinkedMessage].get
    val messageTwoId = messageTwoSaved.id

    val blogWithLinkSetMessages = BlogWithLinkSetMessages("MyBlog", Set(messageOneSaved, messageTwoSaved))
    blogWithLinkSetMessages.save

    val extractedBlogsOpt = orientDb.queryBySql[BlogWithLinkSetMessages]("select from BlogWithLinkSetMessages where name = 'MyBlog'").headOption
    extractedBlogsOpt match {
      case Some(extractedBlog) =>
        inside(extractedBlog) { case BlogWithLinkSetMessages(name, messages) =>
          name should equal("MyBlog")
          messages should have size 2
          messages should contain(LinkedMessage("This is my first message", messageOneId))
          messages should contain(LinkedMessage("last", messageTwoId))
        }
      case None => fail("Model not saved or retrieved")
    }
  }


  test("convert document to object class explicity") {
    val message = LinkedMessage("Hi all")
    val savedMessageDoc = message.save
    val converted = savedMessageDoc.as[LinkedMessage]
    converted should not be empty
    converted should contain(LinkedMessage("Hi all", Option(savedMessageDoc.getIdentity)))

    val broken = savedMessageDoc.as[Complex]
    broken shouldBe empty

    //createOClass[ClassWithOptionalPrimitiveField]

  }


  after {
    dropOClass[Home]
    dropOClass[Brother]
    dropOClass[Family]
    dropOClass[Simple]
    dropOClass[Complex]
    dropOClass[ComplexWithRid]
    dropOClass[LinkedMessage]
    dropOClass[BlogWithLinkedMessage]
    dropOClass[BlogWithLinkSetMessages]
    //dropOClass[ClassWithOptionalPrimitiveField]
  }

}
