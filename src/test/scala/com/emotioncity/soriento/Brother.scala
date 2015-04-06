package com.emotioncity.soriento

import com.orientechnologies.orient.core.record.impl.ODocument
import RichODocumentImpl._

/**
 * Created by stream on 31.03.15.
 */
case class Brother(name: String, kulugda: Option[String])
object Brother {
  implicit object BrotherReader extends ODocumentReader[Brother] {

    def read(oDocument: ODocument): Brother = {
      Brother(
        oDocument.get[String]("mother").get,
        oDocument.get[String]("father")
      )
    }
  }

}
