package com.emotioncity.soriento

import com.orientechnologies.orient.core.db.record.ORecordLazyList
import com.orientechnologies.orient.core.id.ORecordId
import com.orientechnologies.orient.core.metadata.schema.OType
import com.orientechnologies.orient.core.record.impl.ODocument
import org.scalatest.{BeforeAndAfter, Matchers, FunSuite}
import collection.JavaConversions._
import com.orientechnologies.orient.core.db.document.{ODatabaseDocumentPool, ODatabaseDocumentTx}

/**
 * Created by stream on 25.12.14.
 */


class DslTest extends FunSuite with Matchers with BeforeAndAfter with Dsl with ODb {

  implicit val orientDb: ODatabaseDocumentTx =
    ODatabaseDocumentPool.global().acquire("remote:localhost/emotiongraph", "root", "varlogr3_")

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
    val checkinsDocuments: java.util.List[ODocument] = new java.util.ArrayList[ODocument] {{
      add(new ODocument("Checkin").field("location", "Paris"))
      add(new ODocument("Checkin").field("location", "Vladivostok"))
    }}
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

  test("It should update ODocument if field of case class constructor annotated with javax.persistent.Id") {
    createOClass[BlogWithLinkListMessages]
    val blog = BlogWithLinkListMessages(name ="MyBlog", messages = List(Message("Hi"), Message("Here are you?")))
    val blogDoc = productToDocument(blog)
    println("Generated complexDoc: " + blogDoc)
    println(s"Gen, messages field OType: ${blogDoc.fieldType("messages")}")
    println(s"Gen, messages : ${blogDoc.field("messages")}")
    blogDoc.getIdentity.getClusterId should equal(-1)
    blogDoc.getIdentity.getClusterPosition should equal(-1)
    blogDoc.save()
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
    dropOClass[BlogWithLinkListMessages]
    dropOClass[Message]
  }

}
