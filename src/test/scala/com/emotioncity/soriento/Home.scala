package com.emotioncity.soriento

import com.emotioncity.soriento.annotations.Embedded
import com.orientechnologies.orient.core.record.impl.ODocument
import RichODocumentImpl._


/**
 * Created by stream on 31.03.15.
 */
case class Home(name: String, @Embedded family: Family)
object Home {

  implicit object HomeReader extends ODocumentReader[Home] {

    def read(oDocument: ODocument): Home = {
      new Home(
        oDocument.get[String]("name").get,
        oDocument.getAs[Family]("family").get
      )
    }
  }

}