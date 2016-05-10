package com.emotioncity.soriento

import scala.reflect.runtime.universe._

object EnumReflector {
  private def toJavaClass(tpe: Type) = mirror.runtimeClass(tpe.typeSymbol.asClass)

  val mirror = runtimeMirror(this.getClass.getClassLoader)
  private var cache = Map[Type, EnumReflector]()

  def apply(enumElementType: Type): EnumReflector = cache.getOrElse(enumElementType, new EnumReflector(enumElementType))

  final def isEnumeration(tpe: Type): Boolean = tpe.baseClasses.exists(_.fullName == "scala.Enumeration.Value")
  final def isEnumerationValue(obj: Any): Boolean = obj.isInstanceOf[Enumeration$Value]
}


/**
  * Everything you ever wanted to access about a scala.Enumeration through reflection.
  */
class EnumReflector(enumElementType: Type) {

  private val tref = enumElementType.asInstanceOf[TypeRef]
  private val trr: Type = tref.pre
  private val className = trr.typeSymbol.asClass.fullName

  /// The enum's companion object
  val enumObject = getClass.getClassLoader.loadClass(className + "$").getField("MODULE$").get(null)

  val applyMethod = enumObject.getClass.getMethods.filter(m => m.getName.contains("apply"))(0)
  val withNameMethod = enumObject.getClass.getMethods.filter(m => m.getName.contains("withName"))(0)
  val valuesMethod = enumObject.getClass.getMethod("values")

  val rawValues: Iterable[scala.Enumeration$Value] = valuesMethod.invoke(enumObject).asInstanceOf[Iterable[scala.Enumeration$Value]]


  def fromID(enumVal: Int) = values(enumVal) //applyMethod.invoke(enumObject, new Integer(enumVal))

  def fromName(enumName: String): scala.Enumeration$Value = withNameMethod.invoke(enumObject, enumName).asInstanceOf[scala.Enumeration$Value]


  val idMethod = EnumReflector.toJavaClass(enumElementType).getMethod("id")

  def toID(enum: Any): Int = idMethod.invoke(enum).asInstanceOf[Integer].toInt

  def toName(enum: Any): String = enum.toString //nameMethod.invoke(enum).asInstanceOf[String]

  val valueToNames = collection.immutable.TreeMap[Int, String](rawValues.map { x => (toID(x), toName(x)) }.toSeq: _*)
  val values = collection.immutable.TreeMap(rawValues.map { x => (toID(x), x) }.toSeq: _*)
}