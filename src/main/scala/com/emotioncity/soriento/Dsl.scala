package com.emotioncity.soriento

import com.emotioncity.soriento.ReflectionUtils._
import com.orientechnologies.orient.core.metadata.schema.OType
import com.orientechnologies.orient.core.record.impl.ODocument
import com.orientechnologies.orient.core.id.ORID

import scala.reflect.runtime.universe.{Symbol, TypeTag, runtimeMirror}
import scala.collection.JavaConverters._


/**
  * Created by stream on 31.10.14.
  */
trait Dsl {

  private def scalaFieldToDocumentField(field: Any): Any = {
    field match {
      case Some(v) => scalaFieldToDocumentField(v)
      case None => null
      // TODO: Add map here.
      case p: Set[_] => p.map { e => scalaFieldToDocumentField(e) }.asJavaCollection
      case p: List[_] => p.map { e => scalaFieldToDocumentField(e) }.asJavaCollection
      case p: Product if p.productArity > 0 => productToDocument(p)
      case x: Any => x // Builtins and null
      case null => null
    }
  }

  private def initNewDocumentAndID(cc: Product) = {
    // TODO. All this type magic should be cached.
    val modelName = cc.getClass.getSimpleName
    val objectType = ReflectionUtils.erasedObjectType(cc)
    val ctorParams: List[Symbol] = ReflectionUtils.constructor(objectType).symbol.paramLists.flatten
    val fieldList = ctorParams
    val values = cc.productIterator // Are matched to field

    var rid: Option[ORID] = None

    // Collect the (value,field) pairs.
    // Id fields are eliminated since they are provided directly in the document's constructor.
    val purifiedFromIdValuesAndFields = values.zip(fieldList.iterator).toList.filter {
      case (v, f) => {
        if (ReflectionUtils.isId(f)) {
          if (rid.isDefined) throw new Exception(s"Found multiple IDs for ${modelName}")

          v match {
            case null => {} // ID not set
            case None => {} // ID not set
            case Some(id) => rid = Some(id.asInstanceOf[ORID])
            case _ => rid = Some(v.asInstanceOf[ORID])
          }

          false // Remove from purifiedFromIdValuesAndFields
        }
        else true
      }
    }

    // Make the document, with out with a ID
    rid match {
      case Some(id) => (purifiedFromIdValuesAndFields, new ODocument(modelName, id))
      case None => (purifiedFromIdValuesAndFields, new ODocument(modelName))
    }
  }

  /**
    * Is called at db.save to transform a obj into a document.
    *
    * Currently, is limited to saving case classes.
    * The policy is that the cc.productIterator provides the value who's
    * types and annotations are match against the class's constructor
    * parameters to build document field values.
    *
    * You'll be sorry of you override productIterator.
    *
    * Option types, lists, sets, are all handled.
    * ids must habe the type ORID an the annotation @Id.
    * Other annotations in the constructor's parameters are respected.
    */
  implicit def productToDocument[T >: AnyRef](cc: Product): ODocument = {
    // TODO: This is REALLY slow. This work should be cached in a type database.
    val (purifiedFromId, document) = initNewDocumentAndID(cc)

    purifiedFromId.foreach { case (value, field) =>
      val fieldName = field.name.decodedName.toString
      val fieldValue = scalaFieldToDocumentField(value)
      if (fieldValue != null) {
        // Works for polymorphic types?
        val oType: OType = getOType(field)
        //println(s"DOC:${document.getClassName}.${fieldName} = ${fieldValue}    ${oType}")
        document.field(fieldName, fieldValue, oType)
      } else {
        // TODO. Consider making this an exception case
        // Should either declare field as an option, or add a Nullable/Option annotation.
        // println(s"DOC:${document.getClassName}.${fieldName} = NULL")
      }
    }
    document
  }

  /*

    def saveAs[T](implicit reader: ODocumentReader[T], orientDb: ODatabaseDocument): Option[T] = {
      import RichODatabaseDocumentImpl._
      orientDb.saveAs[T](oDocument)
    }*/

}

