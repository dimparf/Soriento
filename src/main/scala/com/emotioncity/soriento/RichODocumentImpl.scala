package com.emotioncity.soriento

import java.util.{List => JList}

import com.orientechnologies.orient.core.record.impl.ODocument

import scala.collection.JavaConversions._


/**
 * Created by stream on 31.03.15.
 */
object RichODocumentImpl {

  implicit class RichODocument(oDocument: ODocument) {

    /**
     * Return ODocument field as case class
     * @param fieldName name of document field
     * @param reader implicit Reader viewed in scope
     * @tparam T return type
     * @return Option[T]
     */
    def getAs[T](fieldName: String)(implicit reader: OReader[_ <: ODocument, T]): Option[T] = {
      get[ODocument](fieldName).flatMap { subDocument =>
        reader match {
          case r: ODocumentReader[T] =>
            r.readOpt(subDocument)
          case _ =>
            None
        }
      }
    }

    /**
     * Return List of case class instances by ODocuemnt field
     * @param fieldName name of document field
     * @param reader implicit reader
     * @tparam T generic type of List
     * @return List[T]
     */
    def listOfEmbedded[T](fieldName: String)(implicit reader: ODocumentReader[T]): List[T] = {
      get[java.util.List[ODocument]](fieldName) match {
        case Some(oDocumentList) =>
          oDocumentList.toList.map { oDocument =>
            reader.readOpt(oDocument)
          }.flatten
        case None =>
          Nil
      }
    }

    /**
     * Return simple field represented as ODocument field
     * @param key name of ODocument
     * @tparam T return type
     * @return T
     */
    def get[T](key: String): Option[T] = Option(oDocument.field(key))
  }

}

