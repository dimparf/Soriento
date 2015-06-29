package com.emotioncity.soriento

import com.emotioncity.soriento.annotations.Embedded
import com.orientechnologies.orient.core.record.impl.ODocument
import RichODocumentImpl._


/**
 * Created by stream on 31.03.15.
 */
case class Blagda(name: String, @Embedded bio: Family)
object Blagda {
  implicit object BlagdaReader extends ODocumentReader[Blagda] {

    def read(oDocument: ODocument): Blagda = {
      new Blagda(
        oDocument.get[String]("name").get,
        oDocument.getAs[Family]("bio").get
      ) with Identifier {
        override def id = oDocument.getIdentity
      }
    }
  }
}