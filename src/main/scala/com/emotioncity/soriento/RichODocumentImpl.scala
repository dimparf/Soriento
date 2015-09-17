package com.emotioncity.soriento

import com.emotioncity.soriento.ReflectionUtils._
import com.orientechnologies.orient.core.metadata.schema.OType
import com.orientechnologies.orient.core.record.impl.ODocument

import scala.collection.JavaConversions._
import scala.reflect.runtime.universe._
import java.util.{List => JList}


/**
 * Created by stream on 31.03.15.
 */
object RichODocumentImpl {

  implicit class RichODocument(oDocument: ODocument)  {

    /**
     * Return ODocument field as case class
     * @param fieldName name of document field
     * @param reader implicit Reader viewed in scope
     * @tparam T return type
     * @return Option[T]
     */
    def getAs[T](fieldName: String)(implicit reader: ODocumentReader[T], tag: TypeTag[T]): Option[T] = {
     /* val tpe = typeOf[T]
      val gen = typeString(tpe)
      println(s"Generic type: $gen")*/
      oDocument.fieldType(fieldName) match {
        case OType.STRING => get[T](fieldName)
        case OType.INTEGER => get[T](fieldName)
        case OType.FLOAT => get[T](fieldName)
        case OType.DOUBLE => get[T](fieldName)
        case OType.BOOLEAN => get[T](fieldName)
        case OType.LONG => get[T](fieldName)
        case OType.SHORT => get[T](fieldName)
        case OType.EMBEDDED =>
          println(s"Embedded read - $fieldName")
          reader.readOpt(oDocument.field[ODocument](fieldName))
        case OType.EMBEDDEDLIST =>
          get[java.util.List[ODocument]](fieldName) match {
            case Some(oDocumentList) =>
              Option(oDocumentList.toList.map(doc => reader.read(doc)).asInstanceOf[T])//TODO STUB, use getAs[T, _ <: Traversable[T]]
            case None =>
              println(s"EmbeddedList not read - $fieldName")
              None
          }
        case OType.EMBEDDEDSET =>
          get[java.util.Set[ODocument]](fieldName) match {
            case Some(oDocumentList) =>
              Option(oDocumentList.toList.map(doc => reader.read(doc)).asInstanceOf[T])
            case None =>
              println(s"EmbeddedSet not read - $fieldName")
              None
          }
        case OType.LINK =>
          println(s"Linked read - $fieldName")
          reader.readOpt(oDocument.field[ODocument](fieldName))
          //TODO YOU WORK HERE ! <<=================
        case OType.ANY =>
          println(s"getAs ANY detected - $fieldName")
          None
        case _ =>
          println(s"getAs unsupported type detected - $fieldName")
          None //unsupported type
        //ANY, BINARY, BOOLEAN, BYTE, CUSTOM, DATE, DATETIME, DECIMAL, EMBEDDEDLIST, EMBEDDEDMAP, EMBEDDEDSET, FLOAT, INTEGER, LINK, LINKBAG, LINKLIST, LINKSET, LONG, SHORT, TRANSIENT
      }
    }

    def getAsList[T](fieldName: String)(implicit reader: ODocumentReader[T]): Option[scala.List[T]] = {
      get[JList[ODocument]](fieldName) match {
        case Some(oDocumentList) =>
          val listOfT: scala.List[T] = oDocumentList.toList.flatMap { oDocument =>
            reader.readOpt(oDocument)
          }
          Option(listOfT)
        case None =>
          None
      }
    }
//
//    def getAsMap[T,U](fieldName: String)(implicit reader: ODocumentReader[T]): Option[Map[T,U]] = {
//      get[java.util.Map JList[ODocument]](fieldName) match {
//        case Some(oDocumentList) =>
//          val listOfT: scala.List[T] = oDocumentList.toList.flatMap { oDocument =>
//            reader.readOpt(oDocument)
//          }
//          Option(listOfT)
//        case None =>
//          None
//      }
//    }

    /**
     * Return simple field represented as ODocument field
     * @param key name of ODocument
     * @tparam T return type
     * @return T
     */
    def get[T](key: String): Option[T] = Option(oDocument.field[T](key))
  }

}