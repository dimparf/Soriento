package com.emotioncity.soriento

import com.emotioncity.soriento.RichODocumentImpl._
import com.emotioncity.soriento.annotations.EmbeddedSet
import com.orientechnologies.orient.core.record.impl.ODocument
import DefaultReaders._


/**
 * Created by stream on 31.03.15.
 */
case class Family(mother: String, father: String, @EmbeddedSet brothers: List[Brother])

object Family {

  implicit object FamilyReader extends ODocumentReader[Family] {

    def read(oDocument: ODocument): Family = {
      new Family(
        oDocument.get[String]("mother").get,
        oDocument.get[String]("father").get,
        oDocument.listOfEmbedded[Brother]("brothers")
      )
    }
  }

}
