package com.emotioncity.soriento

import com.orientechnologies.orient.core.record.impl.ODocument

import scala.collection.JavaConverters._

/**
 * Created by stream on 31.10.14.
 */
trait Dsl {

  implicit def productToDocument[T >: Any](cc: Product): ODocument = {
    val modelName = cc.getClass.getSimpleName
    val document = new ODocument(modelName)
    val values = cc.productIterator
    println("Values " + values.toList)
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
            case _ => x
          }
      }
      if (fieldValue != None) {
        document.field(fieldName, fieldValue) // use case class annotation for detect field OType
      }
    }
    document
  }

  private[this] def isCaseClass(o: Any) = o.getClass.getInterfaces.contains(classOf[scala.Product])

}

