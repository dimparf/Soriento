package com.emotioncity.soriento

import java.util.{List => JList}

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx
import com.orientechnologies.orient.core.record.impl.ODocument

import scala.collection.JavaConversions._


/**
 * Created by stream on 31.03.15.
 */
object RichODocumentImpl {

  implicit class RichODocument(oDocument: ODocument) {

    def getAs[T](fieldName: String)(implicit reader: OReader[_ <: ODocument, T], db: ODatabaseDocumentTx): Option[T] = {
      get[ODocument](fieldName).flatMap { subDocument =>
        reader match {
          case r: ODocumentReader[T] =>
            r.readOpt(subDocument)
          case _ =>
            None
        }
      }
    }

    def listOfEmbedded[T](fieldName: String)(implicit reader: ODocumentReader[T], db: ODatabaseDocumentTx): List[T] = {
      get[java.util.List[ODocument]](fieldName) match {
        case Some(oDocumentList) =>
          oDocumentList.toList.map { oDocument =>
            reader.readOpt(oDocument)
          }.flatten
        case None =>
          Nil
      }
    }

    def get[T](key: String): Option[T] = Option(oDocument.field(key))
  }

}

