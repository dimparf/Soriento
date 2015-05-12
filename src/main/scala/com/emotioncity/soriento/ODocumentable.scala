package com.emotioncity.soriento

import com.emotioncity.soriento.ReflectionUtils.createCaseClass
import com.orientechnologies.orient.core.record.impl.ODocument

import scala.tools.scalap.scalax.rules.scalasig.{SymbolInfoSymbol, SymbolInfo, MethodSymbol, ScalaSigParser,
ClassSymbol => SigClassSymbol}
import scala.reflect.runtime.universe._
import scala.collection.JavaConversions._
import scala.reflect.runtime.universe.TypeTag

/*
 *
 Copyright (c) 2014 Dmitriy Parenskiy aka stream (dimparf@gmail.com)
 */
trait ODocumentable {

  /**
   * If field is not represented in map -> add this field with None value
   * TODO Add to documentation: support for Option and default None values.
   **/
  def fromODocument[T](oDocument: ODocument)(implicit tag: TypeTag[T]) = {
    val t = typeOf[T]
    var map = oDocumentToMap(oDocument)
    val constr = ReflectionUtils.constructor(t)
    val paramsNames = constr.symbol.paramLists.flatten.map(_.name.toString) // get constructor params names
    paramsNames.foreach { name =>
      if (!map.contains(name)) {
        map += name -> None
      }
    }
    createCaseClass[T](map)
  }

  //TODO support linked documents ???
  protected def oDocumentToMap(oDocument: ODocument): Map[String, Any] = {
    val docEntries = oDocument.iterator().toList
    docEntries.map { entry =>
      val key = entry.getKey
      val value = entry.getValue
      value match {
        case document: ODocument =>
          key -> oDocumentToMap(document)
        case _ =>
          key -> value
      }
    }.toMap
  }

}
