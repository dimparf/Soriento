package com.emotioncity.soriento.testmodels

import com.emotioncity.soriento.ODocumentReader
import com.emotioncity.soriento.RichODocumentImpl._
import com.emotioncity.soriento.annotations.LinkSet
import com.orientechnologies.orient.core.record.impl.ODocument

/**
 * Created by stream on 08.09.15.
 */
case class BlogWithLinkSetMessages(name: String, @LinkSet messages: Set[LinkedMessage])

object BlogWithLinkSetMessages {

  implicit object BlogWithLinkSetMessagesReader extends ODocumentReader[BlogWithLinkSetMessages] {

    def read(oDocument: ODocument): BlogWithLinkSetMessages = {
      BlogWithLinkSetMessages(
        oDocument.get[String]("name").get,
        oDocument.getAsSet[LinkedMessage]("messages").get
      )
    }
  }

}