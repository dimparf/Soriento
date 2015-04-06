package com.emotioncity.soriento

import java.util.{List => JList}
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx
import com.orientechnologies.orient.core.db.record.ORecordLazyList
import com.orientechnologies.orient.core.id.ORID

import scala.collection.JavaConversions._
import scala.reflect.runtime.universe.TypeTag
import com.orientechnologies.orient.core.record.impl.ODocument


/**
 * Created by stream on 31.03.15.
 */
object RichODocumentImpl {

  implicit class RichODocument(oDocument: ODocument) {

    def getAs[T](fieldName: String)(implicit reader: OReader[_ <: ODocument, T]): Option[T] = {
      get[ODocument](fieldName).flatMap { subDocument =>
        reader match {
          case r: ODocumentReader[T]@unchecked =>
            subDocument.load("*:3")
            println(s"DocFamily: $subDocument")
            println(s"Brothers: ${subDocument.field[java.util.List[ODocument]]("brothers").getClass}")
            val familyOpt = r.readOpt(subDocument)
            println(s"FamilyOpt? $familyOpt")
            r.readOpt(subDocument)
          case _ => None
        }
      }
    }

    def listOf[T](fieldName: String)(implicit reader: ODocumentReader[T], db: ODatabaseDocumentTx): List[T] = {
      get[java.util.List[ORecordLazyList]](fieldName) match {
        case Some(oDoc) =>
          oDoc.toList.map {oRecordLazyList =>
              oRecordLazyList.toList.map { rid =>
                val doc = db.load(rid.asInstanceOf[ORID])
                println(s"Doc $doc")
                reader.readOpt(doc)
              }.flatten
          }.flatten
        case None => Nil
      }
    }

    def get[T](key: String): Option[T] = Option(oDocument.field(key))
  }

}

