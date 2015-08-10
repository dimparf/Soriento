package com.emotioncity.soriento

import com.orientechnologies.orient.core.record.impl.ODocument
import RichODocumentImpl._
import DefaultReaders._

/**
 * Created by stream on 31.03.15.
 */
case class Brother(name: String, fooBar: Option[String])
object Brother {
  implicit object BrotherReader extends ODocumentReader[Brother] {

    def read(oDocument: ODocument): Brother = {
      new Brother(
        oDocument.get[String]("name").get,
        oDocument.get[String]("fooBar")
      )
    }
  }

}
