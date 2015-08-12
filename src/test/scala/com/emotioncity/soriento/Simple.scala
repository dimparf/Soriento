package com.emotioncity.soriento

import com.emotioncity.soriento.RichODocumentImpl._
import com.orientechnologies.orient.core.record.impl.ODocument

/**
 * Created by stream on 07.07.15.
 */
case class Simple(sField: String)

object Simple {

  implicit object SimpleReader extends ODocumentReader[Simple] {

    def read(oDocument: ODocument): Simple = {
      Simple(
        oDocument.get[String]("sField").get
      )
    }
  }

}