package com.emotioncity.soriento

import java.lang.reflect.{Field, ParameterizedType}
import java.util
import javax.persistence.Id

import com.emotioncity.soriento.annotations._
import com.emotioncity.soriento.loadbyname.ClassNameReadersRegistry
import com.orientechnologies.orient.core.id.ORID
import com.orientechnologies.orient.core.metadata.schema.OType
import com.orientechnologies.orient.core.record.impl.ODocument

import scala.reflect.ClassTag
import scala.reflect.runtime.universe._


/**
  * Created by stream on 14.12.14.
  */
object ReflectionUtils {

  import scala.collection.JavaConverters._

  val mirror = runtimeMirror(getClass.getClassLoader)

  def constructor(t: Type): MethodMirror = {
    val ctr = t.decl(termNames.CONSTRUCTOR)
    mirror.reflectClass(t.typeSymbol.asClass).reflectConstructor(ctr.asMethod)
  }

  def constructorParams(t: Type) = constructor(t).symbol.paramLists.flatten

  def toJavaClass(tpe:Type) = mirror.runtimeClass(tpe.typeSymbol.asClass)

  def toType(clazz: Class[_]): Type = {
    //val mirror = runtimeMirror(clazz.getClassLoader)
    mirror.classSymbol(clazz).toType
  }


  def methods(typ: Type) = typ.members.collect { case m: MethodSymbol => m }.toIndexedSeq.reverse

  def classGetters(typ: Type) = methods(typ).filter(_.isGetter)

  def classAccessors(typ: Type) = methods(typ).filter(_.isAccessor)

  // Is thread-safe. Support polymorphics reading.
  // Caches types and is efficient.
  val readers = ClassNameReadersRegistry()

  def createCaseClass[T](document: ODocument)(implicit tag: TypeTag[T]): T = createCaseClass(document,readers)(tag)

  def createCaseClass[T](document: ODocument, readers:ClassNameReadersRegistry)(implicit tag: TypeTag[T]): T =
    readers.addType(tag.tpe)(document).asInstanceOf[T]


  // return a human-readable type string for type argument 'T'
  // typeString[Int] returns "Int"
  def typeString(t: Type): String = {
    t match {
      case TypeRef(pre, sym, args) =>
        val ss = sym.toString.stripPrefix("trait ").stripPrefix("class ").stripPrefix("type ")
        val as = args.map(typeString)
        if (ss.startsWith("Function")) {
          val arity = args.length - 1
          "(" + as.take(arity).mkString(",") + ")" + "=>" + as.drop(arity).head
        } else {
          if (args.length <= 0) ss else ss + "[" + as.mkString(",") + "]"
        }
    }
  }

  /**
    * Powerful for determine type of Generic
    *
    * @tparam T generic type
    * @return scala.reflect.runtime.universe.Type
    */
  def typeStringByTypeTag[T: TypeTag](t: T): Option[Type] = typeOf[T].typeArgs.headOption

  def typeStringByType(t: Type): Option[Type] = t.typeArgs.headOption

  def erasedObjectType(obj: Any): Type = mirror.reflect(obj).symbol.toType

  def typeOfObject[T](t: T)(implicit tag:TypeTag[T]): Type = tag.tpe

  /**
    * NB. Ordering is reverse of what you might expect.
    * Also, the case accessors hide the annotation information.
    *
    * @param typ
    * @return
    */
  def caseAccessors(typ: Type): Iterable[MethodSymbol] = typ.members.collect {
    case m: MethodSymbol if m.isCaseAccessor => m
  }

  def caseAccessorsFromObject(obj: AnyRef): Iterable[MethodSymbol] = caseAccessors(erasedObjectType(obj))

  def caseAccessorsT[T: TypeTag]() = caseAccessors(typeOf[T])


  def fieldsWithAnnotations(tpe: Type): Option[List[(String, List[Annotation])]] = {
    val companionSymbol = tpe.typeSymbol.companion
    companionSymbol
      .typeSignature
      .members
      .collectFirst { case method: MethodSymbol if method.name.toString == "apply" =>
        method.paramLists.head.map(p => p.name.toString -> p.annotations)
      }
  }

  def onlyFieldsWithAnnotations(tpe: Type): Option[List[(String, List[Annotation])]] = {
    //TODO use top method
    val companionSymbol = tpe.typeSymbol.companion
    companionSymbol
      .typeSignature
      .members
      .collectFirst { case method: MethodSymbol if method.name.toString == "apply" =>
        method.paramLists.head.map(p => p.name.toString -> p.annotations).filter(t => t._2.nonEmpty)
      }
  }

  private def getOTypeBySymbolAnnotationOnly(symbol: Symbol): Option[OType] = {
    // TODO. Validate that symbol.type is Set, List and Maps where specified.
    symbol.annotations.collectFirst {
      case tpe if tpe.tree.tpe =:= typeOf[Embedded] => OType.EMBEDDED
      case tpe if tpe.tree.tpe =:= typeOf[Linked] => OType.LINK

      case tpe if tpe.tree.tpe =:= typeOf[LinkSet] => OType.LINKSET
      case tpe if tpe.tree.tpe =:= typeOf[LinkList] => OType.LINKLIST
      //TODO: case tpe if tpe.tree.tpe =:= typeOf[LinkMap] => OType.LINKMAP

      case tpe if tpe.tree.tpe =:= typeOf[EmbeddedSet] => OType.EMBEDDEDSET
      case tpe if tpe.tree.tpe =:= typeOf[EmbeddedList] => OType.EMBEDDEDLIST
      //TODO: case tpe if tpe.tree.tpe =:= typeOf[EmbeddedMap] => OType.EMBEDDEDMAP

    }
  }

  /**
    * Ignores any annotations. Prefer to use getOType(parameterSymbol: Symbol) where possible
    * since this will allow specialization by annotation.
    */
  def getOTypeByType(typ: Type): OType = {
    typ match {
      case tpe if tpe <:< typeOf[Option[_]] => getOTypeByType(tpe.typeArgs(0))
      case tpe if EnumReflector.isEnumeration(typ) => OType.INTEGER
      case tpe if typ <:< typeOf[Boolean] => OType.BOOLEAN
      case tpe if typ <:< typeOf[Int] => OType.INTEGER
      case tpe if typ <:< typeOf[Long] => OType.LONG
      case tpe if typ <:< typeOf[Short] => OType.SHORT
      case tpe if typ <:< typeOf[Double] => OType.DOUBLE
      case tpe if typ <:< typeOf[Float] => OType.FLOAT
      //case tpe if typ <:< typeOf[Char] => OType.CHARACTER
      case tpe if typ <:< typeOf[String] => OType.STRING
      case tpe if typ <:< typeOf[java.lang.Boolean] => OType.BOOLEAN
      case tpe if typ <:< typeOf[java.lang.Integer] => OType.INTEGER
      case tpe if typ <:< typeOf[java.lang.Long] => OType.LONG
      case tpe if typ <:< typeOf[java.lang.Short] => OType.SHORT
      case tpe if typ <:< typeOf[java.lang.Double] => OType.DOUBLE
      case tpe if typ <:< typeOf[java.lang.Float] => OType.FLOAT
      case tpe if typ <:< typeOf[java.lang.Byte] => OType.BYTE
      case tpe if typ <:< typeOf[java.lang.Math] => OType.DECIMAL
      case tpe if typ <:< typeOf[Array[Byte]] => OType.BINARY
      //case tpe if typ <:< typeOf[java.lang.Character] => OType.CHARACTER
      case tpe if typ <:< typeOf[java.util.Date] => OType.DATE
      // case tpe if typ <:< typeOf[java.util.Date] => OType.DATETIME  // Should prefer this. Will be better precision
      case _ =>  OType.ANY
    }
  }

  def getOType(parameterSymbol: Symbol): OType = {
    getOTypeBySymbolAnnotationOnly(parameterSymbol) match {
      case Some(annotationOType) => annotationOType
      case None => getOTypeByType(parameterSymbol.asTerm.typeSignature)
    }
  }

  def getOType(inName: String, field: Field, clazz: Class[_]): OType = {
    val sym: Option[Symbol] = getScalaFieldSymbol(inName, toType(clazz))
    getOType(sym.get)  // Symbol MUST exist
  }



  def hasAnnotation(symbol: Symbol, annotation: Type) = symbol.annotations.exists(_.tree.tpe =:= annotation)

  def isId(symbol: Symbol) = hasAnnotation(symbol: Symbol, typeOf[Id])

  def isId(name: String, clazz: Class[_]): Boolean = {
    val typeOfClass = toType(clazz)
    val maybeFieldsWithAnnotations: Option[List[(String, List[Annotation])]] = onlyFieldsWithAnnotations(typeOfClass)
    maybeFieldsWithAnnotations match {
      case Some(fieldsWithAnnotations) =>
        fieldsWithAnnotations.exists(pair => pair._1 == name && pair._2.exists(annotation => annotation.tree.tpe =:= typeOf[Id]))
      case None => false
    }
  }

  /**
    * Get RID if it present in object
    * TODO: More type safe!
    *
    * @param cc case class
    * @return None if RID does not exist else Some(rid)
    */
  def rid(cc: Product): Option[ORID] = {
    val clazz = cc.getClass
    val fieldList: List[Field] = cc.getClass.getDeclaredFields.toList
    val idFieldOpt = fieldList.find(field => isId(field.getName, clazz))
    idFieldOpt match {
      case Some(idField) =>
        idField.setAccessible(true)
        getGenericTypeClass(idField) match {
          case Some(generic) => //Option[ORID]
            idField.get(cc).asInstanceOf[Option[ORID]]
          case None => //ORID
            toType(idField.getType) match {
              case tpe if tpe =:= typeOf[ORID] =>
                Option(idField.get(cc).asInstanceOf[ORID])
              case _ =>
                None
            }
        }
      case None =>
        None
    }
  }

  /**
    * Do not use this method. It is broken, return None for boxing java types (Double, Boolean, etc)
    *
    * @param field field of constructor
    * @return
    */
  private[soriento] def getGenericTypeClass(field: Field): Option[Class[_]] = {
    val genericType = field.getGenericType
    genericType match {
      case parametrizedType: ParameterizedType =>
        val parameterType = parametrizedType.getActualTypeArguments()(0)
        parameterType match {
          case value: Class[_] =>
            Option(value)
          case _ =>
            None
        }
      case _ =>
        None
    }
  }

  def getScalaFieldSymbol(fieldName: String, typ: Type): Option[Symbol] = constructorParams(typ).find( _.name.toString == fieldName )

  def getScalaFieldType(fieldName: String, clazz: Class[_]): Option[Type] = getScalaFieldType( fieldName, toType(clazz) )

  def getScalaFieldType(fieldName: String, typ: Type): Option[Type] = getScalaFieldSymbol(fieldName, typ).map{ p=>p.typeSignature }

  def getScalaGenericTypeClass(fieldName: String, clazz: Class[_]): Option[Type] = {
    getScalaFieldType(fieldName, clazz) match {
      case None => None
      case Some(fullType) => fullType.typeArgs.headOption
    }
  }


  /**
    * Option[Option[T]] => T
    */
  def removeOptionType(typ:Type): Type = {
    typ match {
      case tpe if tpe <:< typeOf[Option[_]] => removeOptionType(tpe.typeArgs(0))
      case _ => typ
    }
  }


  //  {
//    val tpe = getTypeForClass(clazz)
//
//    val paramOpt: Option[Option[Symbol]] = tpe.companion.typeSymbol
//      .typeSignature
//      .members
//      .collectFirst { case method: MethodSymbol if method.name.toString == "apply" =>
//        method.paramLists.head.find(p => p.name.toString == fieldName)
//      }
//
//    paramOpt match {
//      case Some(p) =>
//        p.flatMap(s => s.typeSignature.resultType.typeArgs.headOption)
//      case None =>
//        None
//    }
//
//  }

  def isCaseClass(tpe: Type) =
    tpe.typeSymbol.isClass && tpe.typeSymbol.asClass.isCaseClass

}
