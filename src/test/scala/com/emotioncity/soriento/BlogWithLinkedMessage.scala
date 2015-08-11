package com.emotioncity.soriento

import com.emotioncity.soriento.annotations.{Linked, Embedded}
import com.orientechnologies.orient.core.record.impl.ODocument
import com.emotioncity.soriento.RichODocumentImpl._


/**
 * Created by stream on 10.08.15.
 */
case class BlogWithLinkedMessage(name: String, @Linked message: Message)

object BlogWithLinkedMessage {
  implicit object BlogWithLinkedMessagesReader extends ODocumentReader[BlogWithLinkedMessage] {

    def read(oDocument: ODocument): BlogWithLinkedMessage = {
      println(s"Message field is: ${oDocument.field("message")}")
      BlogWithLinkedMessage(
        oDocument.get[String]("name").get,
        oDocument.getAs[Message]("message").get
      )
    }
  }
}