package com.emotioncity.soriento.loadbyname

import com.emotioncity.soriento.ReflectionUtils
import com.orientechnologies.orient.core.record.impl.ODocument

import scala.reflect.runtime.universe.{Type, TypeTag}

/**
  * Registers readers. Is a map from document class names (provided by the DB)
  * to a loader function.
  * classNamer decides on the name of classes.
  * Not sure how generic should be handled. It seems sufficient to specific
  * "Generic" as the class name for Generic[T] since any object of T can be
  * returned type-erased, and the document provides sufficient information
  * to create a field of type T without knowing T. (ClassToNameFunctions.simple
  * is probably right for generics).
  *
  * Currently, reflectionUtils.createCaseClass[T] requires a concrete class.
  * It would be better to make this reflectionUtils.createClass(document): Any
  */
case class ClassNameReadersRegistry(classNamer: (Type => String) =
                           ClassToNameFunctions.simple) {
  //                               ClassToNameFunctions.underscoreTypeParameters) {

  type DocToObjectFunc = (ODocument => Any)

  var _readers = Map[String, DocToObjectFunc]()
  var _types = Map[String, TypeTag[_]]()

  def readers = _readers

  /**
    * Register a reader for type T
    *
    * @param tag
    * @tparam T
    * @return
    */
  def add[T](implicit tag: TypeTag[T]): DocToObjectFunc = {
    tag.tpe.typeSymbol.asClass // A class is expected
    val name = classNamer(tag.tpe)

    _types.get(name) match {
      case Some(existingTag) => {
        if (existingTag.tpe =:= tag.tpe) {
          _readers(name)
        }
        throw new Exception(s"name '${name}' for type ${tag} was already for type ${existingTag}")
      }
      case None => {
        val factory = ReflectionUtils.createCaseClass[T](_)
        //println(s"Registered ${name} for ${tag}")
        _readers += (name -> factory)
        _types += (name -> tag)
        factory
      }
    }
  }
}
