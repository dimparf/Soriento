package com.emotioncity.soriento

import com.emotioncity.soriento.testmodels._
import com.orientechnologies.orient.core.metadata.schema.OType
import org.scalatest.{BeforeAndAfter, FunSuite, Matchers}


/**
 *
 * Created by stream on 14.12.14.
 */


class ODbTest extends FunSuite with Matchers with BeforeAndAfter with ODb {

  val schema = orientDb.getMetadata.getSchema

  after {
    dropOClass[Test]
    dropOClass[Message]
    dropOClass[BlogWithLinkedMessage]
    dropOClass[BlogWithEmbeddedMessages]
    dropOClass[BlogWithLinkSetMessages]
  }

  test("ODb should be create OClass with name in OrientDb") {
    createOClass[Test]
    assert(schema.existsClass("Test"))
    assert(schema.getClass("Test").existsProperty("field"))
  }

  test("ODb should be create OClass case classes with @Linked or @Embedded annotated fields") {
    createOClass[BlogWithLinkedMessage]
    assert(schema.existsClass("BlogWithLinkedMessage"))
    assert(schema.existsClass("Message"))
    assert(schema.getClass("BlogWithLinkedMessage").existsProperty("message"))
    val linkedMessageProperty = schema.getClass("BlogWithLinkedMessage").getProperty("message").getType
    linkedMessageProperty should equal(OType.LINK)

    createOClass[BlogWithEmbeddedMessages]
    assert(schema.existsClass("BlogWithEmbeddedMessages"))
    assert(schema.existsClass("Message"))
    assert(schema.getClass("BlogWithEmbeddedMessages").existsProperty("message"))
    val embeddedMessageProperty = schema.getClass("BlogWithEmbeddedMessages").getProperty("message").getType
    embeddedMessageProperty should equal(OType.EMBEDDED)
  }

  test("ODb should be create OClass by case classes with @LinkSet type of connections") {
    createOClass[BlogWithLinkSetMessages]
    assert(schema.existsClass("BlogWithLinkSetMessages"))
    assert(schema.existsClass("LinkedMessage"))
    assert(schema.getClass("BlogWithLinkSetMessages").existsProperty("messages"))
    val linkedMessageProperty = schema.getClass("BlogWithLinkSetMessages").getProperty("messages").getType
    linkedMessageProperty should equal(OType.LINKSET)
  }

  test("ODb should be drop OClass from OrientDb") {
    dropOClass[Test]
    dropOClass[Message]
    dropOClass[BlogWithLinkedMessage]
    dropOClass[BlogWithEmbeddedMessages]
    schema.existsClass("Test") should not be true
    schema.existsClass("Message") should not be true
    schema.existsClass("BlogWithLinkedMessage") should not be true
    schema.existsClass("BlogWithEmbeddedMessages") should not be true

    createOClass[Test]
    val dropped = dropOClass[Test]
    dropped should equal(true)
    val droppedAfterDropped = dropOClass[Test]
    droppedAfterDropped should equal(false)
  }

  test("ODb should be create schema without field annotated with @javax.persistent.Id") {
    createOClass[BlogWithEmbeddedListMessages]
    schema.existsClass("BlogWithEmbeddedListMessages")
    val oClass = schema.getClass("BlogWithEmbeddedListMessages")
    oClass.getProperty("id") should be(null)
    oClass.getProperty("messages") should not be null
    oClass.getProperty("name") should not be null
    dropOClass[BlogWithEmbeddedListMessages]
    dropOClass[Message]
  }

}
