package com.emotioncity.soriento

import java.util.{List => JList}

import com.emotioncity.soriento.ReflectionUtils._
import com.orientechnologies.orient.core.metadata.schema.OType
import com.orientechnologies.orient.core.record.impl.ODocument

import scala.collection.JavaConversions._
import scala.reflect.runtime.universe._


/**
 * Created by stream on 31.03.15.
 */
object RichODocumentImpl {

  implicit class RichODocument(oDocument: ODocument)  {

    import DefaultReaders._
    import DefaultReaders.collectionToReader
    /**
     * Return ODocument field as case class
     * @param fieldName name of document field
     * @param reader implicit Reader viewed in scope
     * @tparam T return type
     * @return Option[T]
     */
    def getAs[T](fieldName: String)(implicit reader: ODocumentReader[T], tag: TypeTag[T]): Option[T] = {
      val tpe = typeOf[T]
      val gen = typeString(tpe)
      println(s"Generic type: $gen")
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
              Option(oDocumentList.toList.map(doc => reader.read(doc)).asInstanceOf[T])//STUB
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
        case OType.ANY =>
          println(s"getAs ANY detected - $fieldName")
          None
        case _ =>
          println(s"getAs unsupported type detected - $fieldName")
          None //unsupported type
        //ANY, BINARY, BOOLEAN, BYTE, CUSTOM, DATE, DATETIME, DECIMAL, EMBEDDEDLIST, EMBEDDEDMAP, EMBEDDEDSET, FLOAT, INTEGER, LINK, LINKBAG, LINKLIST, LINKSET, LONG, SHORT, TRANSIENT
      }
    }

    /**
     * Return List of case class instances by ODocuemnt field
     * @param fieldName name of document field
     * @param reader implicit reader
     * @tparam T generic type of List
     * @return List[T]
     *         Find fieldName in case class and determinate it Type
     *         Use information of Type for convert Type to ODocument or it part
     */
    def listOfEmbedded[T](fieldName: String)(implicit reader: ODocumentReader[T], tag: TypeTag[T]): List[T] = {
      get[java.util.List[ODocument]](fieldName) match {
        case Some(oDocumentList) =>
          oDocumentList.toList.flatMap { oDocument =>
            reader.readOpt(oDocument)
          }
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
    def get[T](key: String): Option[T] = Option(oDocument.field[T](key))
  }

}