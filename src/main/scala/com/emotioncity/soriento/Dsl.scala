package com.emotioncity.soriento

import com.emotioncity.soriento.ReflectionUtils._
import com.orientechnologies.orient.core.record.impl.ODocument

import scala.collection.JavaConverters._


/**
 * Created by stream on 31.10.14.
 */
trait Dsl {

  implicit def productToDocument[T >: Any](cc: Product): ODocument = {
    val modelName = cc.getClass.getSimpleName
    //println(s"Product name: $modelName")
    val ridOpt = rid(cc)
    val document = if (ridOpt.isDefined) new ODocument(modelName, ridOpt.get) else new ODocument(modelName)
    //println(s"document rid: ${document.getIdentity}")
    val values = cc.productIterator
    val fieldList = cc.getClass.getDeclaredFields.toList
    fieldList.foreach { field =>
      val fieldName = field.getName
      val fieldValue = values.next() match {
        case p: Product if p.productArity > 0 =>
          p match {
            case Some(value) =>
              if (isCaseClass(value)) {
                productToDocument(value.asInstanceOf[Product])
              } else {
                value
              }
            case _: List[_] =>
              p.asInstanceOf[List[_]].map {
                case cc: Product =>
                  productToDocument(cc)
                case item =>
                  item
              }.asJavaCollection
            case _ => productToDocument(p)
          }
        case x =>
          x match {
            case _: Set[_] =>
              x.asInstanceOf[Set[_]].map {
                case cc: Product =>
                  productToDocument(cc)
                case item =>
                  item
              }.asJavaCollection
                case Nil => None //TODO fix empty list matching
                case _ => x
          }
      }
      if (fieldValue != None) {
        val oType = getOType(fieldName, field, field.getDeclaringClass)
        //println(s"Field metadata: Name: $fieldName, Value: $fieldValue, OType: $oType")
        document.field(fieldName, fieldValue, oType)
      }
    }
    document
  }

  private[this] def isCaseClass(o: Any) = o.getClass.getInterfaces.contains(classOf[scala.Product])

}

