package com.emotioncity.soriento

import java.lang.reflect.Field

import com.emotioncity.soriento.ReflectionUtils._
import com.orientechnologies.orient.core.db.ODatabaseRecordThreadLocal
import com.emotioncity.soriento.RichODatabaseDocumentImpl._
import com.orientechnologies.orient.core.db.document.ODatabaseDocument
import com.orientechnologies.orient.core.exception.OSchemaException
import com.orientechnologies.orient.core.metadata.schema.{OClass, OSchema, OType}

import scala.reflect.ClassTag

trait ODb {
  var register: Map[String, OClass] = Map.empty

  def initialize() {}

  def createOClass[T](implicit tag: ClassTag[T], db: ODatabaseDocument): OClass = {
    db.activateOnCurrentThread()
    val schema = db.getMetadata.getSchema
    val clazz = tag.runtimeClass
    val ccSimpleName = clazz.getSimpleName
    if (!schema.existsClass(ccSimpleName)) {
      createOClassByName(schema, clazz.getName, ccSimpleName)
    } else {
      schema.getClass(ccSimpleName)
    }
  }


  /**
    * Drop OClass if it exists
    * @param tag
    * @param db
    * @tparam T Associated case class
    * @return true if class dropped else false
    */
  def dropOClass[T](implicit tag: ClassTag[T], db: ODatabaseDocument): Boolean = {
    db.activateOnCurrentThread()
    try {
      val oClassName = tag.runtimeClass.getSimpleName
      db.getMetadata.getSchema.dropClass(oClassName)
      register -= oClassName
      true
    } catch {
      case ose: OSchemaException => false
    }
  }

  private[soriento] def createOClassByName(schema: OSchema, ccName: String, ccSimpleName: String): OClass = {
    if (!register.contains(ccSimpleName) && !schema.existsClass(ccSimpleName)) {
      val oClass = schema.createClass(ccSimpleName)
      register += ccSimpleName -> oClass
      val clazz: Class[_] = Class.forName(ccName)
      val fieldList = clazz.getDeclaredFields.toList
      val nameTypeMap: Map[String, Field] = fieldList.map(field => field.getName -> field).toMap
      for (entity <- nameTypeMap) {
        val (name, field) = entity
        val oType = getOType(name, field, clazz)
        if (oType == OType.LINK || oType == OType.LINKLIST || oType == OType.LINKSET || oType == OType.LINKMAP
          || oType == OType.EMBEDDED || oType == OType.EMBEDDEDLIST || oType == OType.EMBEDDEDSET) {
          val genericOpt = getScalaGenericTypeClass(name, clazz) //getGenericTypeClass(field)
          val subOClassName = if (genericOpt.isDefined) genericOpt.get.typeSymbol.fullName else field.getType.getName
          val subOClassSimpleName = subOClassName.substring(subOClassName.lastIndexOf(".") + 1)
          if (register.contains(subOClassName)) {
            oClass.createProperty(name, oType, register.get(subOClassSimpleName).get)
          } else {
            val subOClass = createOClassByName(schema, subOClassName, subOClassSimpleName)
            oClass.createProperty(name, oType, subOClass)
            register += subOClassSimpleName -> subOClass
          }
        } else {
          if (!isId(name, clazz)) {
            oClass.createProperty(name, oType)
          }
        }
      }
      oClass
    } else {
      register.get(ccSimpleName).get
    }
  }


}
