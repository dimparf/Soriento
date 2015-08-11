package com.emotioncity.soriento

import javax.persistence.Id

import com.emotioncity.soriento.RichODocumentImpl._
import com.emotioncity.soriento.annotations.{Embedded, EmbeddedList}
import com.orientechnologies.orient.core.id.ORID
import com.orientechnologies.orient.core.record.impl.ODocument


/**
 * Created by stream on 10.08.15.
 */
case class ComplexWithRid(
  @Id id: ORID,
  iField: Int,
  @Embedded simple: Simple,
  sField: String,
  @EmbeddedList listField: List[Simple])

object ComplexWithRid {

  implicit object ComplexWithRidReader extends ODocumentReader[ComplexWithRid] {

    def read(oDocument: ODocument): ComplexWithRid = {
      ComplexWithRid(
        oDocument.getIdentity,
        oDocument.get[Int]("iField").get,
        oDocument.getAs[Simple]("simple").get,
        oDocument.get[String]("sField").get,
        oDocument.getAsList[Simple]("listField").get
      )
    }
  }

}

