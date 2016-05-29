package com.emotioncity.soriento

import com.orientechnologies.orient.core.db.document.ODatabaseDocument
import com.orientechnologies.orient.core.exception.OSchemaException
import com.orientechnologies.orient.core.metadata.schema.{OClass, OSchema, OType}
import scala.reflect.runtime.universe.{Type, TypeTag, Symbol, typeOf}

import scala.reflect.ClassTag

trait ODb {
  var register: Map[String, OClass] = Map.empty

  def initialize() {}

  def createOClass[T <: AnyRef](implicit tag: TypeTag[T], db: ODatabaseDocument): OClass = {
    createOClass(tag.tpe, db)
  }

  def createOClass(typ: Type, db: ODatabaseDocument): OClass = {
    db.activateOnCurrentThread()
    val schema = db.getMetadata.getSchema
    val clazz = ReflectionUtils.toJavaClass(typ)
    val ccSimpleName = clazz.getSimpleName

    if (!schema.existsClass(ccSimpleName)) {
      createOClassByType(schema, typ)
    } else {
      schema.getClass(ccSimpleName)
    }
  }


  /**
    * Drop OClass if it exists
    *
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

  private[soriento] def createOClassByType(schema: OSchema, typ: Type): OClass = {
    val clazz = ReflectionUtils.toJavaClass(typ)
    val ccSimpleName = clazz.getSimpleName

    register.get(ccSimpleName) match {
      case Some(oclass) => oclass
      case None => {

        val oClass = schema.createClass(ccSimpleName)
        // Prevent recursive definition
        register += ccSimpleName -> oClass

        val fields: List[Symbol] = if (clazz.isInterface) List.empty[Symbol] else ReflectionUtils.constructorParams(typ)

        fields
          .filter(!ReflectionUtils.isId(_)) // IDs saved explicitly
          .foreach { fieldSymbol =>

          val fieldName = fieldSymbol.name.toString
          val fieldType = ReflectionUtils.removeOptionType(fieldSymbol.typeSignature)


          val oType = ReflectionUtils.getOType(fieldSymbol)

          oType match {

            case OType.LINK
                 | OType.EMBEDDED =>
              oClass.createProperty(fieldName, oType, createOClassByType(schema, fieldType))

            case OType.LINKLIST
                 | OType.EMBEDDEDLIST
                 | OType.LINKSET
                 | OType.EMBEDDEDSET => {
              val genericOpt = fieldType.typeArgs.head // MUST be a generic type
              oClass.createProperty(fieldName, oType, createOClassByType(schema, genericOpt))
            }

            case OType.EMBEDDEDMAP
                 | OType.LINKMAP => {
              throw new IllegalArgumentException("MAP not implemented")
              // Need to test this

              // Map MUST be a generic type
              val genericOpt1 = fieldType.typeArgs(0)
              val genericOpt2 = fieldType.typeArgs(1)
              if (genericOpt1 <:< typeOf[String]) new IllegalArgumentException(s"Map key must be string in field ${ccSimpleName}.${fieldName}")

              oClass.createProperty(fieldName, oType, createOClassByType(schema, genericOpt2))
            }
            case OType.BOOLEAN
                 | OType.INTEGER
                 | OType.SHORT
                 | OType.LONG
                 | OType.FLOAT
                 | OType.DOUBLE
                 | OType.DATETIME // Date.class
                 | OType.STRING
                 | OType.BYTE // Byte.class
                 | OType.DATE // Date.class
                 | OType.BINARY // byte[].class
                 | OType.DECIMAL // BigDecimal.class
                 | OType.ANY =>

              oClass.createProperty(fieldName, oType)


            case OType.TRANSIENT // ("Transient", 18, null, new Class<?>[] {}),
                 | OType.CUSTOM // ("Custom", 20, OSerializableStream.class, new Class<?>[] { OSerializableStream.class, Serializable.class }),
                 | OType.LINKBAG // ("LinkBag", 22, ORidBag.class, new Class<?>[] { ORidBag.class }),
                 | _ =>
              throw new IllegalArgumentException(s"Unexpected otype '${oType}' for field ${ccSimpleName}.${fieldName}")

          }
        }

        oClass
      }
    }
  }
}
