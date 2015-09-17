package com.emotioncity.soriento

import com.orientechnologies.orient.core.db.document.{ODatabaseDocumentPool, ODatabaseDocumentTx}
import com.orientechnologies.orient.core.metadata.schema.OType
import com.orientechnologies.orient.core.record.impl.ODocument
import com.orientechnologies.orient.core.sql.query.OSQLSynchQuery
import org.scalatest.{BeforeAndAfter, FunSuite, Matchers}

import scala.collection.JavaConversions._

/**
 * Created by stream on 25.12.14.
 */


class DslTest extends FunSuite with Matchers with BeforeAndAfter with Dsl with ODb {

  implicit val orientDb: ODatabaseDocumentTx =
    ODatabaseDocumentPool.global().acquire("remote:localhost/emotiongraph", "root", "poweron")

  test("Dsl should be convert Product to ODocument") {
    val blog = Blog(author = "Arnold", message = Record("Agrh!"))
    val blogDoc = productToDocument(blog)
    blogDoc.getClassName should equal("Blog")
    val blogMessageField = blogDoc.field[ODocument]("message")

    blogMessageField.getClassName should equal("Record")
    blogMessageField.field[String]("content") should equal("Agrh!")

    val blogAuthorField = blogDoc.field("author").toString
    blogAuthorField should equal("Arnold")
    val recordContentField = blogDoc.field[ODocument]("message").field[String]("content").toString

    recordContentField should equal("Agrh!")
  }

  test("Dsl should be convert Product with list fields to ODocument") {
    val checkin1 = Checkin("Paris")
    val checkin2 = Checkin("Vladivostok")
    val user = User("Elena", List(checkin1, checkin2))
    val userDoc = productToDocument(user)
    val checkinsDocuments: java.util.List[ODocument] = new java.util.ArrayList[ODocument] {
      {
        add(new ODocument("Checkin").field("location", "Paris"))
        add(new ODocument("Checkin").field("location", "Vladivostok"))
      }
    }
    val expectedDocument = new ODocument("User")
      .field("name", "Elena")
      .field("checkins", checkinsDocuments, OType.EMBEDDEDLIST).save()

    userDoc.getClassName should equal("User")
    userDoc.field[String]("name") should equal("Elena")
    userDoc.fieldType("checkins") should equal(OType.EMBEDDEDLIST) //TODO Support other OTypes with annotations
    //println("Checkins value: " + userDoc.field[ORecordLazyList]("checkins"))
    //val checkinsFromExpected =  userDoc.field[ORecordLazyList]("checkins").iterator().toList
    //TODO implement it test
  }

  test("productToDocument saves OType by annotation of type") {
    val user = User("Dmitriy", List(Checkin("Paris"), Checkin("Saint Francisco")))
    val userDoc = productToDocument(user)
    userDoc.fieldType("name") should equal(OType.STRING)
    userDoc.fieldType("checkins") should equal(OType.EMBEDDEDLIST)
  }

  test("Should save case class instances with schema-full mode") {
    createOClass[BlogWithEmbeddedListMessages]
    val blog = BlogWithEmbeddedListMessages(name = "MyBlog", messages = List(Message("Hi"), Message("Here are you?")))
    val blogDoc = productToDocument(blog)
    blogDoc.save()
  }


  test("It should update ODocument if field of case class constructor annotated with javax.persistent.Id") {
    createOClass[BlogWithEmbeddedListMessages]
    val blog = BlogWithEmbeddedListMessages(name = "MyBlog", messages = List(Message("Hi"), Message("Here are you?")))
    val blogDoc = productToDocument(blog)
    blogDoc.getIdentity.getClusterId should equal(-1)
    blogDoc.getIdentity.getClusterPosition should equal(-1)
    val savedDoc = blogDoc.save
    savedDoc.getIdentity.getClusterId should not equal (-1)
    savedDoc.getIdentity.getClusterPosition should not equal (-1)

    val extractedDoc = orientDb
      .query[java.util.List[ODocument]](new OSQLSynchQuery[ODocument](s"select from ${savedDoc.getIdentity}"), Nil: _*).head
    extractedDoc.field[String]("name") should equal(savedDoc.field[String]("name"))
    val extractedMessages = extractedDoc.field[java.util.List[ODocument]]("messages")
    extractedMessages.filter(document => document.field[String]("text") == "Hi") should not be empty
    extractedMessages.filter(document => document.field[String]("text") == "Here are you?") should not be empty
    savedDoc.getIdentity should equal(extractedDoc.getIdentity)
    val simpleUpdateBlog = BlogWithEmbeddedListMessages(Option(extractedDoc.getIdentity), name = "Super blog", messages = List(Message("Hi"), Message("Here are you?")))
    val updatedBlogIdentity = simpleUpdateBlog.save.getIdentity
    val updatedBlog = orientDb
      .query[java.util.List[ODocument]](new OSQLSynchQuery[ODocument](s"select from $updatedBlogIdentity"), Nil: _*).head
    updatedBlog.getIdentity.getClusterId should equal(savedDoc.getIdentity.getClusterId)
    updatedBlog.getIdentity.getClusterPosition should equal(savedDoc.getIdentity.getClusterPosition)
    updatedBlog.field[String]("name") should equal("Super blog")
  }


  test("Simple field test") {
    val simple = Simple("TesT")
    val simpleDoc = productToDocument(simple)
    simpleDoc.field[String]("sField") should equal("TesT")
    simpleDoc.fieldType("sField") should equal(OType.STRING)
  }


  after {
    dropOClass[Blog]
    dropOClass[Record]
    dropOClass[User]
    dropOClass[Checkin]
    dropOClass[Simple]
    dropOClass[BlogWithEmbeddedListMessages]
    dropOClass[Message]
  }

}
