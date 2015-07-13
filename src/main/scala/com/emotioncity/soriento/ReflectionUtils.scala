package com.emotioncity.soriento

import java.lang.reflect.Field

import com.emotioncity.soriento.annotations.{EmbeddedSet, LinkSet, Linked, Embedded}
import com.orientechnologies.orient.core.metadata.schema.OType

import scala.reflect.ClassTag
import scala.tools.scalap.scalax.rules.scalasig.{SymbolInfoSymbol, SymbolInfo, MethodSymbol => MethodSymbolX, ScalaSigParser, ClassSymbol => SigClassSymbol}

import scala.reflect.runtime.universe._


/**
 * Created by stream on 14.12.14.
 */
object ReflectionUtils {

 def constructor(t: Type): MethodMirror = {
    val m = runtimeMirror(getClass.getClassLoader)
    m.reflectClass(t.typeSymbol.asClass).reflectConstructor(t.decl(termNames.CONSTRUCTOR).asMethod)
  }

  /*def constructorNew(t: Type): Option[MethodMirror] = {
    val m = runtimeMirror(getClass.getClassLoader)
    t.decl(
      termNames.CONSTRUCTOR
    ).asTerm.alternatives.collect {
      case ctor: MethodSymbol if ctor.isCaseAccessor => ctor
    }.map(ms => m.reflectClass(t.typeSymbol.asClass).reflectConstructor(ms)).headOption
  }*/

  def createCaseClass[T](map: Map[String, Any])(implicit tag: TypeTag[T]): T = {
    val tpe = typeOf[T]
    createCaseClassByType(tpe, map).asInstanceOf[T]
  }

  def createCaseClassByType(tpe: Type, map : Map[String, Any]): Any = {
    val constr = constructor(tpe)
    val params = constr.symbol.paramLists.flatten // get constructor params
    val input = map.map {
      case (k: String, m: Map[String, Any]) =>
        k -> createCaseClassByType(params.find(_.name.toString == k).get.typeSignature, m)
      case x => x
    }
    constr(params.map(_.name.toString).map(input).toSeq: _*) // invoke constructor
  }

  def valNamesWithAnnotations[T](tag: ClassTag[T]): List[(String, List[java.lang.annotation.Annotation])] = {
    val clazz = tag.runtimeClass
    valNamesWithAnnotations(clazz)
  }

  //TODO Improve this Method
  def valNamesWithAnnotations[T](clazz: Class[_]): List[(String, List[java.lang.annotation.Annotation])] = {
    val ctors = clazz.getConstructors

    assert(ctors.size == 1, "Class " + clazz.getName + " should have only one constructor")
    val sig = ScalaSigParser.parse(clazz).getOrElse(sys.error("No ScalaSig for class " + clazz.getName + ", make sure it is a top-level case class"))

    val classSymbol = sig.parseEntry(0).asInstanceOf[SigClassSymbol]
    assert(classSymbol.isCase, "Class " + clazz.getName + " is not a case class")

    val tableSize = sig.table.size
    val ctorIndex = (1 until tableSize).find { i =>
      sig.parseEntry(i) match {
        case m@MethodSymbolX(SymbolInfo("<init>", owner, _, _, _, _), _) => owner match {
          case sym: SymbolInfoSymbol if sym.index == 0 => true
          case _ => false
        }
          case _ => false
      }
    }.getOrElse(sys.error("Cannot find constructor entry in ScalaSig for class " + clazz.getName))

    val paramsListBuilder = List.newBuilder[String]
    for (i <- (ctorIndex + 1) until tableSize) {
      sig.parseEntry(i) match {
        case MethodSymbolX(SymbolInfo(name, owner, _, _, _, _), _) => owner match {
          case sym: SymbolInfoSymbol if sym.index == ctorIndex => paramsListBuilder += name
          case _ =>
        }
          case _ =>
      }
    }

    val paramAnnoArr = ctors(0).getParameterAnnotations
    val builder = List.newBuilder[(String, List[java.lang.annotation.Annotation])]

    val paramIter = paramsListBuilder.result().iterator
    val annotationIterator = paramAnnoArr.iterator

    while (paramIter.hasNext && annotationIterator.hasNext) {
      builder += ((paramIter.next(), annotationIterator.next().toList))
    }

    builder.result().filter { case (name, annotationList) => annotationList.nonEmpty}
  }

  // return a human-readable type string for type argument 'T'
  // typeString[Int] returns "Int"
  def typeString(t: Type): String = {
    t match { case TypeRef(pre, sym, args) =>
      val ss = sym.toString.stripPrefix("trait ").stripPrefix("class ").stripPrefix("type ")
      val as = args.map(typeString)
      if (ss.startsWith("Function")) {
        val arity = args.length - 1
        "(" + (as.take(arity).mkString(",")) + ")" + "=>" + as.drop(arity).head
      } else {
        if (args.length <= 0) ss else (ss + "[" + as.mkString(",") + "]")
      }
    }
  }

  /**
   * Powerful for determine type of Generic
   * @tparam T generic type
   * @return scala.reflect.runtime.universe.Type
   */
  def typeStringByTypeTag[T: TypeTag] = typeOf[T].typeArgs.head


 /* def getAsS[T](fieldName: String)(implicit tag: TypeTag[T]) = { //Option[T], List[T], value
  val fieldValue = oDocument.field(fieldName)
    println("Field value: " + fieldValue.getClass)
    val constr = constructor(typeOf[T])
    val params = constr.symbol.paramLists.flatten // get constructor params
    params.find(_.name.toString == fieldName) match {
      case Some(symbol) =>
        symbol.typeSignature match {
          case tpe if typeOf[Boolean] =:= tpe =>
          case tpe if typeOf[java.lang.String] =:= tpe =>
          case tpe if typeOf[Int] =:= tpe =>
          case tpe if typeOf[Float] =:= tpe =>
          case tpe if typeOf[Double] =:= tpe =>
          case tpe if typeOf[Byte] =:= tpe =>
            get[T](fieldName)
          case tpe if typeOf[List[T]] =:= tpe =>
            listOfEmbedded[T](fieldName)
          case tpe if typeOf[String] =:= tpe =>
            getAs[T](fieldName)
          case _ => get[T](fieldName)

        }
      case None => None //TODO what???
    }
  }*/

  def getOType[T](inName: String, field: Field)(implicit tag: ClassTag[T]): OType = {
    getOType(inName, field, tag.runtimeClass)
  }

  def getOType[T](inName: String, field: Field, clazz: Class[_]): OType = {
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
