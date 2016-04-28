package com.emotioncity.soriento.loadbyname

import scala.reflect.runtime.universe.{Type,TypeRef}

/**
  * Policies for naming types to ODB.
  */
object ClassToNameFunctions {
  private def dropPath(s: String) = s.splitAt(s.lastIndexOf(".") + 1)._2

  def simple(tpe: Type): String = tpe.typeSymbol.asClass.name.toString

  //def underscoreTypeParameters( tag:TypeTag[_] ) : String = dropPath(tag.toString).replace("[","_").replace("]", "")

  private def parameterListToString(types: List[Type]): String = {
    if (types.size == 0) ""
    else s"[${types.map(shortGenericName(_)).mkString(",")}]"
  }

  /**
    * "List[Int]"
    */
  private def shortGenericName(typ: Type): String = {
    val tref = typ.asInstanceOf[TypeRef]
    val root = tref.typeSymbol.asClass.name
    s"${root}${parameterListToString(tref.args)}"
  }

  def underscoreTypeParameters(typ: Type) = {
    shortGenericName(typ).replace("[", "_").replace("]", "")
  }
}
