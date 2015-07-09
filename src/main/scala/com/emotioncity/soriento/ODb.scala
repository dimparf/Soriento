package com.emotioncity.soriento

import java.lang.reflect.Field

import com.emotioncity.soriento.ReflectionUtils._
import com.orientechnologies.orient.core.db.document.ODatabaseDocument
import com.orientechnologies.orient.core.exception.OSchemaException
import com.orientechnologies.orient.core.metadata.schema.{OClass, OSchema, OType}

import scala.reflect.ClassTag

/**
 * Created by stream on 13.12.14.
 *
 */
trait ODb {
  private var register: Map[String, OClass] = Map.empty

  def initialize() {}

  def createOClass[T](implicit tag: ClassTag[T], db: ODatabaseDocument): OClass = {
    val schema = db.getMetadata.getSchema
    val clazz = tag.runtimeClass
    val ccSimpleName = clazz.getSimpleName
    if (!schema.existsClass(ccSimpleName)) {
      //TODO isExists ???
      createOClassByName(schema, clazz.getName, ccSimpleName)
    } else schema.getClass(ccSimpleName)
  }


  /**
   * Drop OClass if it exists
   * @param tag
   * @param db
   * @tparam T Associated case class
   * @return true if class dropped else false
   */
  def dropOClass[T](implicit tag: ClassTag[T], db: ODatabaseDocument) = {
    try {
      db.getMetadata.getSchema.dropClass(tag.runtimeClass.getSimpleName)
      true
    } catch {
      case ose: OSchemaException =>
        false
    }
  }

  private def createOClassByName(schema: OSchema, ccName: String, ccSimpleName: String): OClass = {
    if (!register.contains(ccSimpleName)) {
      val oClass = schema.createClass(ccSimpleName)
      val clazz = Class.forName(ccName)
      val fieldList = clazz.getDeclaredFields.toList
      val nameTypeMap: Map[String, Field] = fieldList.map(field => field.getName -> field).toMap
      for (entity <- nameTypeMap) {
        val (name, field) = entity
        val oType = getOType(name, field, clazz)
        if (oType == OType.LINK || oType == OType.LINKLIST || oType == OType.LINKMAP
          || oType == OType.EMBEDDED || oType == OType.EMBEDDEDLIST || oType == OType.EMBEDDEDSET) {
          val subOClassName = field.getType.getSimpleName
          if (register.contains(subOClassName)) {
            oClass.createProperty(name, oType, register.get(subOClassName).get)
          } else {
            val subOClass = createOClassByName(schema, field.getType.getName, field.getType.getSimpleName)
            oClass.createProperty(name, oType, subOClass)
            register += subOClassName -> subOClass
          }
        } else {
          oClass.createProperty(name, oType)
        }
      }
      oClass
    } else {
      register.get(ccSimpleName).get
    }
  }

}
