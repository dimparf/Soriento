package com.emotioncity.soriento

import com.orientechnologies.orient.core.record.impl.ODocument
import org.scalatest.{BeforeAndAfter, Matchers, FunSuite}

import com.orientechnologies.orient.core.db.document.{ODatabaseDocumentPool, ODatabaseDocumentTx}

/**
 * Created by stream on 25.12.14.
 */


class DslTest extends FunSuite with Matchers with BeforeAndAfter with Dsl with ODb {

  implicit val orientDb: ODatabaseDocumentTx =
    ODatabaseDocumentPool.global().acquire("remote:localhost/emotioncity", "root", "varlogr3_")

  test("Dsl should be convert Any with Product to ODocument") {
    val blog = Blog(author = "Arnold", message = Record("Agrh!"))
    val blogDoc = productToDocument(blog)
    val blogMessageField = blogDoc.field[ODocument]("message")

    blogMessageField should be equals new ODocument("Record").field("content", "Agrh!")

    val blogAuthorField = blogDoc.field("author").toString
    blogAuthorField should be equals "Arnold"
    val recordContentField = blogDoc.field[ODocument]("message").field[String]("content").toString

    recordContentField should be equals "Agrh"
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
    val expectedDocument = new ODocument("User").field("name", "Elena").field("checkins", checkinsDocuments)
    userDoc should be equals expectedDocument
  }

  after {
    dropOClass[Blog]
    dropOClass[Record]
    dropOClass[User]
    dropOClass[Checkin]
  }

  def initialize() = ???
}
