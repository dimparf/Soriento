package com.emotioncity.soriento

import com.emotioncity.soriento.RichODocumentImpl._
import com.orientechnologies.orient.core.record.impl.ODocument

/**
 * Created by stream on 10.08.15.
 */
case class Message(text: String)

object Message {

  implicit object MessageReader extends ODocumentReader[Message] {

    def read(oDocument: ODocument): Message = {
      Message(
        oDocument.get[String]("text").get
      )
    }
  }

}