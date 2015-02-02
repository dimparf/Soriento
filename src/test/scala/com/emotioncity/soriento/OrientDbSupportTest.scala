package com.emotioncity.soriento

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx
import com.orientechnologies.orient.core.record.impl.ODocument
import org.scalatest.{FunSuite, Matchers}
import com.orientechnologies.orient.core.db.document.{ODatabaseDocumentPool, ODatabaseDocumentTx}
/**
 * Created by stream on 25.12.14.
 */
class OrientDbSupportTest extends FunSuite with Matchers with ODb with Transactions {

  def initialize() {}
  implicit var db: ODatabaseDocumentTx = new ODatabaseDocumentTx("remote:localhost/emotioncity").open("root", "varlogr3_")

  test("OrientDbSupport should be execute query and return list of model") {
    //dropOClass[Blog]
    //dropOClass[Record]
    //createOClass[Blog]
    //val blog = Blog(author = "John", message = Record("Hi this is my first post"))
    //blog.save
    //blog2.save()
    //createOClass[Message]
    //val oMessageDoc1 = new ODocument("Message").field("text", "Hi All")
    //val oMessageDoc2 = new ODocument("Message").field("text", "Happy New Year!")
    //transaction {
      //oMessageDoc1.save()
      //oMessageDoc2.save()
    //}
    //println("After transaction")
    //val messages = orientDb.queryBySql[Message]("select from Message") // IT'S WORK!
    //println("Messages: " + messages)
    //blogs should be equals List(
    //Blog(author = "John", message = Record("Hi this is my first post")),
    //Blog(author = "Tim", message = Record("Hi Tim!")))
  }

}
