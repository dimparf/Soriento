package com.emotioncity.soriento

import scala.reflect.runtime.universe._

object EnumReflector {
  private def toJavaClass(tpe: Type) = mirror.runtimeClass(tpe.typeSymbol.asClass)

  val mirror = runtimeMirror(this.getClass.getClassLoader)
  private var cache = Map[Type, EnumReflector]()

  def apply(enumElementType: Type): EnumReflector = cache.getOrElse(enumElementType, new EnumReflector(enumElementType))

  final def isEnumeration(sym: Symbol): Boolean = isEnumeration(sym.typeSignature)

  final def isEnumeration(tpe: Type): Boolean = tpe.baseClasses.exists(_.fullName == "scala.Enumeration.Value")

  final def isEnumerationValue(obj: Any): Boolean = obj.isInstanceOf[Enumeration$Value]
}


/**
  * Everything you ever wanted to access about a scala.Enumeration through reflection.
  *
  * BUGS. Fails unless the enum was declared in package scope. (no object or inner-class or function-scope enums)
  */
class EnumReflector(val enumElementType: Type) {

  private val tref = enumElementType.asInstanceOf[TypeRef]
  private val enumType: Type = tref.pre
  val className = enumType.typeSymbol.asClass.fullName

  /// The enum's companion object
  getClass.getClassLoader.loadClass(className)
  val enumObject = getClass.getClassLoader.loadClass(className + "$").getField("MODULE$").get(null)

  val applyMethod = enumObject.getClass.getMethods.filter(m => m.getName.contains("apply"))(0)
  val withNameMethod = enumObject.getClass.getMethods.filter(m => m.getName.contains("withName"))(0)
  val valuesMethod = enumObject.getClass.getMethod("values")

  val rawValues: Iterable[scala.Enumeration$Value] = valuesMethod.invoke(enumObject).asInstanceOf[Iterable[scala.Enumeration$Value]]


  def fromID(enumVal: Int) = idToEnumValue(enumVal) //applyMethod.invoke(enumObject, new Integer(enumVal))

  def fromName(enumName: String): scala.Enumeration$Value = withNameMethod.invoke(enumObject, enumName).asInstanceOf[scala.Enumeration$Value]


  val idMethod = EnumReflector.toJavaClass(enumElementType).getMethod("id")

  def toID(enum: Any): Int = idMethod.invoke(enum).asInstanceOf[Integer].toInt

  def toName(enum: Any): String = enum.toString //nameMethod.invoke(enum).asInstanceOf[String]

  lazy val idToName = collection.immutable.TreeMap[Int, String](rawValues.map { x => (toID(x), toName(x)) }.toSeq: _*)
  lazy val idToEnumValue = collection.immutable.TreeMap(rawValues.map { x => (toID(x), x) }.toSeq: _*)

  private def hasEnumerationReturnType(s: MethodSymbol) =
    s.isPublic &&
      s.returnType.typeSymbol.isClass &&
      s.returnType.typeSymbol.asClass.baseClasses.filter(_.fullName == "scala.Enumeration.Value").size > 0

  /**
    * Finds the name of the enum as its named as a member of the enum class.
    */
  lazy val memberNameToEnumValue = collection.immutable.TreeMap[String, scala.Enumeration$Value](
    ReflectionUtils.classGetters(enumType)
      //.filter(s => s.isPublic && s.returnType <:< typeOf[scala.Enumeration$Value])  // Nope!
      .filter(hasEnumerationReturnType)
      .map { s =>
        val memberName = s.name.toString
        val enumValue = enumObject.getClass.getMethod(memberName).invoke(enumObject).asInstanceOf[scala.Enumeration$Value]
        println(s"${memberName} ${enumValue}")
        (memberName -> enumValue)
      }: _*
  )

  lazy val idToMemberName = memberNameToEnumValue.map { case (name, enum) => (toID(enum) -> name) }
}