package com.emotioncity.soriento

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx
import com.orientechnologies.orient.core.metadata.schema.OType
import org.scalatest.{Matchers, FunSuite, BeforeAndAfter}


/**
 * Created by stream on 14.12.14.
 */


class ODbTest extends FunSuite with Matchers with BeforeAndAfter with ODb {
  implicit val db: ODatabaseDocumentTx =
    new ODatabaseDocumentTx("remote:localhost/emotioncity").open("root", "varlogr3_")
  val schema = db.getMetadata.getSchema

  override def initialize(): Unit = {}

  after {
    dropOClass[Test]
    dropOClass[BlogWithLinkedMessages]
    dropOClass[BlogWithEmbeddedMessages]
    dropOClass[BlogWithLinkSetMessages]
  }

  test("ODb should be create OClass with name in OrientDb") {
    createOClass[Test]
    assert(schema.existsClass("Test"))
    assert(schema.getClass("Test").existsProperty("field"))
  }

  test("ODb should be create OClass graph by case classes with @Linked or @Embedded annotated fields") {
    createOClass[BlogWithLinkedMessages]
    assert(schema.existsClass("BlogWithLinkedMessages"))
    assert(schema.existsClass("Message"))
    assert(schema.getClass("BlogWithLinkedMessages").existsProperty("message"))
    val linkedMessageProperty = schema.getClass("BlogWithLinkedMessages").getProperty("message")
    linkedMessageProperty should be equals OType.LINK

    createOClass[BlogWithEmbeddedMessages]
    assert(schema.existsClass("BlogWithEmbeddedMessages"))
    assert(schema.existsClass("Message"))
    assert(schema.getClass("BlogWithEmbeddedMessages").existsProperty("message"))
    val embeddedMessageProperty = schema.getClass("BlogWithEmbeddedMessages").getProperty("message")
    embeddedMessageProperty should be equals OType.EMBEDDED
  }

  test("ODb should be create OClass by case classes with @LinkSet type of connections") {
    createOClass[BlogWithLinkSetMessages]
    assert(schema.existsClass("BlogWithLinkSetMessages"))
    assert(schema.existsClass("Message"))
    assert(schema.getClass("BlogWithLinkSetMessages").existsProperty("messages"))
    val linkedMessageProperty = schema.getClass("BlogWithLinkSetMessages").getProperty("messages")
    linkedMessageProperty should be equals OType.LINKSET
  }

  test("ODb should be drop OClass from OrientDb") {
    dropOClass[Test]
    dropOClass[Message]
    dropOClass[BlogWithLinkedMessages]
    dropOClass[BlogWithEmbeddedMessages]
    schema.existsClass("Test") should not be true
    schema.existsClass("Message") should not be true
    schema.existsClass("BlogWithLinkedMessages") should not be true
    schema.existsClass("BlogWithEmbeddedMessages") should not be true

    createOClass[Test]
    val dropped = dropOClass[Test]
    dropped should be equals true
    val droppedAfterDropped = dropOClass[Test]
    droppedAfterDropped should be equals false
  }

}
