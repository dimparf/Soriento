package com.emotioncity.soriento

import java.lang.reflect.Field

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx
import com.orientechnologies.orient.core.metadata.schema.{OClass, OSchema, OType}

import scala.reflect.ClassTag
import scala.tools.scalap.scalax.rules.scalasig._

/**
 * Created by stream on 13.12.14.
 */
trait ODb {
  private var register: Map[String, OClass] = Map.empty

  def initialize()

  def createOClass[T](implicit tag: ClassTag[T], db: ODatabaseDocumentTx): OClass = {
    val schema = db.getMetadata.getSchema
    val clazz = tag.runtimeClass
    val ccSimpleName = clazz.getSimpleName
    if (!schema.existsClass(ccSimpleName)) {//TODO isExists ???
      createOClassByName(schema, clazz.getName, ccSimpleName)
    } else schema.getClass(ccSimpleName)
  }

  def dropOClass[T](implicit tag: ClassTag[T], db: ODatabaseDocumentTx) = {
    db.getMetadata.getSchema.dropClass(tag.runtimeClass.getSimpleName)
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
        if (oType == OType.LINK || oType == OType.LINKLIST || oType == OType.LINKMAP || oType == OType.EMBEDDED) {
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

  private def getOType[T](inName: String, field: Field)(implicit tag: ClassTag[T]): OType = {
    getOType(inName, field, tag.runtimeClass)
  }

  private def getOType[T](inName: String, field: Field, clazz: Class[_]): OType = {
    import ReflectionUtils._
    val fieldClassName = field.getType.getName
    println(s"Field type: $fieldClassName")
    fieldClassName match {
      //TODO add support for List[T]
      case "java.lang.Boolean" | "boolean"=> OType.BOOLEAN
      case "java.lang.String" | "string" => OType.STRING
      case "java.lang.Byte" | "byte" => OType.BYTE
      case "java.lang.Short" | "short" => OType.SHORT
      case "java.lang.Integer" | "int" => OType.INTEGER
      case "java.lang.Long" | "long" => OType.LONG
      case "java.lang.Float" | "float" => OType.FLOAT
      case "java.lang.Double" | "double" => OType.DOUBLE
      case "java.util.Date" => OType.DATE
      case _ =>
        val annotatedFields: List[(String, List[java.lang.annotation.Annotation])] = valNamesWithAnnotations(clazz)
        annotatedFields.find {
          case (name, listOfAnnotations) => name == inName
        }.map {
          case (name, listOfAnnotations) => listOfAnnotations(0) // LINK or EMBEDDED
        } match {
          case Some(annotation) =>
            annotation match {
              case a: com.emotioncity.soriento.Embedded => OType.EMBEDDED
              case a: com.emotioncity.soriento.Linked => OType.LINK
              case _ => OType.ANY
            }
          case None => OType.ANY
        }
    }
  }


}
