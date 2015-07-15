package com.emotioncity.soriento

import javax.persistence.Id

import com.emotioncity.soriento.RichODocumentImpl._
import com.emotioncity.soriento.annotations.{Embedded, EmbeddedSet}
import com.orientechnologies.orient.core.id.ORecordId
import com.orientechnologies.orient.core.record.impl.ODocument

/**
 * Created by stream on 07.07.15.
 */
case class Complex(iField: Int, @Embedded simple: Simple, sField: String, @EmbeddedSet listField: List[Simple], @Id id: Option[ORecordId] = None)

object Complex {

  implicit object ComplexReader extends ODocumentReader[Complex] {

    def read(oDocument: ODocument): Complex = {
      new Complex(
        oDocument.getAsS[String]("iField").asInstanceOf[Int],
        oDocument.getAsS[Simple]("simple").asInstanceOf[Simple],
        oDocument.getAsS[String]("sField").asInstanceOf[String],
        oDocument.getAsS[Simple]("listField").asInstanceOf[List[Simple]],
        oDocument.getAsS[ORecordId]("@RID")
      )
    }
  }

}

