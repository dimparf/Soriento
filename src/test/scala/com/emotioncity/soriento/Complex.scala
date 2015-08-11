package com.emotioncity.soriento

import javax.persistence.Id

import com.emotioncity.soriento.RichODocumentImpl._
import com.emotioncity.soriento.annotations.{EmbeddedList, Embedded, EmbeddedSet}
import com.orientechnologies.orient.core.id.ORecordId
import com.orientechnologies.orient.core.record.impl.ODocument

/**
 * Created by stream on 07.07.15.
 */
case class Complex(iField: Int, @Embedded simple: Simple, sField: String, @EmbeddedList listField: List[Simple])

object Complex {

  implicit object ComplexReader extends ODocumentReader[Complex] {

    def read(oDocument: ODocument): Complex = {
      new Complex(
        oDocument.get[Int]("iField").get,
        oDocument.getAs[Simple]("simple").get,
        oDocument.get[String]("sField").get,
        oDocument.getAsList[Simple]("listField").get
      )
    }
  }

}

