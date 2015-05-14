package com.emotioncity.soriento

import java.lang.reflect.Field

import com.emotioncity.soriento.annotations.{LinkSet, Linked, EmbeddedSet, Embedded}
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx
import com.orientechnologies.orient.core.exception.OSchemaException
import com.orientechnologies.orient.core.metadata.schema.{OClass, OSchema, OType}

import scala.reflect.ClassTag

/**
 * Created by stream on 13.12.14.
 *
 */
trait ODb {
  private var register: Map[String, OClass] = Map.empty

  def initialize()

  def createOClass[T](implicit tag: ClassTag[T], db: ODatabaseDocumentTx): OClass = {
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
  def dropOClass[T](implicit tag: ClassTag[T], db: ODatabaseDocumentTx) = {
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

  private def getOType[T](inName: String, field: Field)(implicit tag: ClassTag[T]): OType = {
    getOType(inName, field, tag.runtimeClass)
  }

  private def getOType[T](inName: String, field: Field, clazz: Class[_]): OType = {
    import com.emotioncity.soriento.ReflectionUtils._
    val fieldClassName = field.getType.getName
    println(s"Field type: $fieldClassName")
    fieldClassName match {
      //TODO add support for List[T]
      case "java.lang.Boolean" | "boolean" => OType.BOOLEAN
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
          case (name, listOfAnnotations) => listOfAnnotations.head // LINK or EMBEDDED or LINKSET
        } match {
          case Some(annotation) =>
            annotation match {
              case a: Embedded => OType.EMBEDDED
              case a: Linked => OType.LINK
              case a: LinkSet
                if fieldClassName == "scala.collection.immutable.Set" || fieldClassName == "scala.collection.immutable.List" => OType.LINKSET //TODO what types is supported?
              case a: EmbeddedSet
                if fieldClassName == "scala.collection.immutable.Set" || fieldClassName == "scala.collection.immutable.List" => OType.EMBEDDEDSET
              case _ => OType.ANY //TODO unsupported annotations
            }
          case None => OType.ANY
        }
    }
  }


}
