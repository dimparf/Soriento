package com.emotioncity.soriento

import scala.reflect.ClassTag
import scala.tools.scalap.scalax.rules.scalasig.{SymbolInfoSymbol, SymbolInfo, MethodSymbol, ScalaSigParser,
ClassSymbol => SigClassSymbol}
import scala.reflect.runtime.universe._


/**
 * Created by stream on 14.12.14.
 */
object ReflectionUtils {

  def constructor(t: Type) = {
    val m = runtimeMirror(getClass.getClassLoader)
    m.reflectClass(t.typeSymbol.asClass).reflectConstructor(t.decl(termNames.CONSTRUCTOR).asMethod)
  }

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
        case m@MethodSymbol(SymbolInfo("<init>", owner, _, _, _, _), _) => owner match {
          case sym: SymbolInfoSymbol if sym.index == 0 => true
          case _ => false
        }
          case _ => false
      }
    }.getOrElse(sys.error("Cannot find constructor entry in ScalaSig for class " + clazz.getName))

    val paramsListBuilder = List.newBuilder[String]
    for (i <- (ctorIndex + 1) until tableSize) {
      sig.parseEntry(i) match {
        case MethodSymbol(SymbolInfo(name, owner, _, _, _, _), _) => owner match {
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
}
