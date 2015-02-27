package com.emotioncity.soriento

import java.util

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx
import com.orientechnologies.orient.core.record.impl.ODocument
import org.scalatest.{Matchers, FunSuite}

/**
 * Created by stream on 25.12.14.
 */


class ODocumentableTest
  extends FunSuite with Matchers with ODocumentable {

  implicit val db: ODatabaseDocumentTx =
    new ODatabaseDocumentTx("remote:localhost/emotioncity").open("root", "varlogr3_")

  test("ODocumentable should be convert ODocument to Map") {
    val oDocument = new ODocument("Blog")
    oDocument.field("author", "Tim")
    oDocument.field("message", new ODocument("Record").field("content", "Hi!"))
    val mapFromODocument = oDocumentToMap(oDocument)

    mapFromODocument should be equals Map("author" -> "Tim", "message" -> Map("content" -> "Hi!"))
  }

  test("ODocumentable should be create case class instance by document") {
    val oDocument = new ODocument("Blog")
    oDocument.field("author", "Alice")
    oDocument.field("message", new ODocument("Record").field("content", "Hi!"))
    val blogFromODocument = fromODocument[Blog](oDocument)
    
    blogFromODocument should be equals Blog("Alice", Record("Hi!"))

    val oBlogWithLinkSetMessages = new ODocument("BlogWithLinkSetMessages")
    oBlogWithLinkSetMessages.field("name", "Rabbit blog")
    val messages = new util.ArrayList[ODocument]()
    messages.add(new ODocument("Message"))
    messages.add(new ODocument("Message2"))
    oBlogWithLinkSetMessages.field("messages", messages)

    println(s"oBlogWithLinkSetMessages: $oBlogWithLinkSetMessages")

    val blogWithLinkSetMessagesFromODocument = fromODocument[BlogWithLinkSetMessages](oDocument)

    blogWithLinkSetMessagesFromODocument should be equals BlogWithLinkSetMessages("Rabbit blog", Set(Message("Test"), Message("Test2")))



  }

}
