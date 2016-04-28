package com.emotioncity.soriento.loadbyname

import java.util
import com.emotioncity.soriento.ReflectionUtils
import _root_.com.orientechnologies.orient.core.record.impl.ODocument
import _root_.com.orientechnologies.orient.core.id.ORID

import scala.collection.JavaConverters._
import scala.collection.{Map, mutable}
import scala.reflect.runtime.universe.{Type, TypeTag, runtimeMirror, typeOf, Symbol}

object Typedefs {
  // Extracts a scala object from a document
  type DocumentReader = (ODocument => Any)

  // Extracts a specific field from a document
  type FieldReader = (ODocument => Any)

  // Maps from document field values into the type parameters need for a class constructor.
  type ValueReader = (Any => Any)

}

import Typedefs._

/**
  * Reads a document and emits a scala type.
  *
  * @param tpe               Just for debugging.
  * @param fieldConstructors Returns constructor fields for this type from an odocument.
  */
case class DocumentFromConstructor(val tpe: Type,
                                   val fieldConstructors: Seq[FieldReader]
                                  ) extends DocumentReader {
  val constr = ReflectionUtils.constructor(tpe)

  def apply(doc: ODocument): Any = {
    // TODO: constructing a list here is still quite inefficient.
    // Can avoid this? (Pool of arrays?)

    val prms = fieldConstructors.map(fc => fc(doc))
    constr(prms: _*) // invoke constructor
  }
}

/**
  * Registry for a set of readers that map ODocuments to objects. Specific readers
  * are looked-up by the document's class name
  *
  * classNamer maps Types to class names.
  *
  * Not sure how generics should be handled. It seems probably sufficient to specify
  * "Generic" as the class name for Generic[T] since any object of T can be
  * returned type-erased, and the document provides sufficient information
  * to create a field of type T without knowing T. (ClassToNameFunctions.simple
  * is probably right for generics).
  *
  */
case class ClassNameReadersRegistry(val classNamer: (Type => String) = ClassToNameFunctions.simple) {
  //                               ClassToNameFunctions.underscoreTypeParameters) {

  private val mirror = runtimeMirror(getClass.getClassLoader)

  // Maps from classes to readers.
  private var _readers = Map[String, DocumentReader]()
  private var _classNameToType = Map[String, Type]()

  def readers = _readers

  /**
    * Register a document reader for type T
    *
    * @param tag
    * @tparam T
    * @return
    */
  def add[T](implicit tag: TypeTag[T]): DocumentReader = addType(tag.tpe)

  def addType(tpe: Type): DocumentReader = {
    val name = classNamer(tpe)

    _classNameToType.get(name) match {
      case Some(existingType) => {
        if (existingType =:= tpe) _readers(name)
        else throw new Exception(s"name '${name}' for type ${tpe} was already registered for type ${existingType}")
      }
      case None => {
        val reader = makeDocumentReaderFromType(tpe)

        this.synchronized {
          _readers += (name -> reader)
          _classNameToType += (name -> tpe)
        }

        reader
      }
    }
  }


  /**
    * Convert a oDocument into a scala object.
    * A reader is looked-up based on the document classname
    *
    * @param document
    * @return
    */
  def createClassByDocumentClassName(document: ODocument): Any = {
    val name = document.getClassName
    _readers.get(name) match {
      case Some(reader) => reader(document)
      case None => throw new Exception(s"Document classname '${name}' has no reader")
    }
  }

  private def makeDocumentReaderFromType(tpe: Type): DocumentReader = {

    val constructor = ReflectionUtils.constructor(tpe)
    val constructorParams = constructor.symbol.paramLists.flatten

    // TODO: Add ID.

    val constructorFieldReaders = constructorParams.map(symbol => toFieldReader(symbol))

    DocumentFromConstructor(tpe, constructorFieldReaders)
  }


  private val ORIDType = typeOf[ORID]

  private def toFieldReader(field: Symbol): FieldReader = {
    val name: String = field.name.decodedName.toString
    val tpe: Type = field.typeSignature

    if (ReflectionUtils.isId(field)) {
      tpe match {
        case tpe if tpe <:< typeOf[Option[ORID]] => {
          // Read and option type
          {
            doc: ODocument => {
              val value = doc.getIdentity
              if (value == null) None
              else Some[Any](value)
            }
          }
        }
        case tpe if tpe <:< typeOf[ORID] => {
          doc: ODocument => doc.getIdentity
        }
        case _ => throw new IllegalArgumentException(s"@Id field must be type ORID or Option[ORID]")
      }
    }
    else {
      // Other option types handled here.
      val valueReader = getValueMapperForRead(tpe)

      {
        doc: ODocument => valueReader(doc.field(name))
      }
    }
  }

  /**
    * For a given type typ return a function that converts into that type
    * from what is held in a document.
    *
    * A read time we know the full type name (and type) of the class being read.
    * Therefore to get the fastest possible deserialization (without
    * byte code compilation, we build a list of functions
    * that maps document fields into constructor parameters.
    *
    * @param typ
    * @return
    */
  private def getValueMapperForRead(typ: Type): ValueReader = {

    // The ordering of these cases is important.
    // Map should precede sets, which should precede other sequences/iterables
    // collection.mutable should precede collection.immutable, which should precede collection.

    // TODO. Manage null values for container fields.

    typ match {

      // Builtin types
      case typ if typ <:< typeOf[Boolean] ||
        typ <:< typeOf[Int] ||
        typ <:< typeOf[Long] ||
        typ <:< typeOf[Short] ||
        typ <:< typeOf[Double] ||
        typ <:< typeOf[Float] ||
        typ <:< typeOf[Char] ||
        typ <:< typeOf[String] ||
        typ <:< typeOf[java.lang.Boolean] ||
        typ <:< typeOf[java.lang.Integer] ||
        typ <:< typeOf[java.lang.Long] ||
        typ <:< typeOf[java.lang.Short] ||
        typ <:< typeOf[java.lang.Double] ||
        typ <:< typeOf[java.lang.Float] ||
        typ <:< typeOf[java.lang.Character] ||
        typ <:< typeOf[java.util.Date]
      => {
        value: Any => value // Unboxing will do its magic
      }

      // Option fields
      case typ if typ <:< typeOf[Option[_]] => {
        val elemReader = getValueMapperForRead(typ.typeArgs.head)

        {
          value: Any =>
            if (value == null) None
            else Some(elemReader(value))
        }
      }


      //Maps
      //Maps: immutable
      case typ if
      typ <:< typeOf[collection.immutable.HashMap[String, _]] ||
        typ <:< typeOf[collection.immutable.Map[String, _]] => {
        value: Any =>

          val elems: collection.immutable.Map[String, Any] = value.asInstanceOf[util.Map[String, ODocument]].asScala.mapValues(v => createClassByDocumentClassName(v)).toMap
          elems.asInstanceOf[collection.immutable.Map[String, Any]] // Type check
      }

      //Maps: mutable
      case typ if
      typ <:< typeOf[collection.mutable.HashMap[String, _]] ||
        typ <:< typeOf[collection.mutable.Map[String, _]] => {
        value: Any =>

          val elems = value.asInstanceOf[util.Map[String, ODocument]].asScala.map(kv => (kv._1 -> createClassByDocumentClassName(kv._2)))
          elems.asInstanceOf[collection.mutable.HashMap[String, Any]] // Type check
      }
      //Maps: misc
      case typ if
      typ <:< typeOf[collection.Map[String, _]] => {
        value: Any =>
          val elems: mutable.Map[String, Any] = value.asInstanceOf[util.Map[String, ODocument]].asScala.map(kv => (kv._1 -> createClassByDocumentClassName(kv._2)))
          elems
      }

      //Sets: immutable
      case typ if
      //typ <:< typeOf[collection.immutable.HashSet[_]] ||
      typ <:< typeOf[collection.immutable.Set[_]] => {
        value: Any =>
          val elems: Seq[Any] = value.asInstanceOf[util.Set[ODocument]].asScala.toSeq.map(doc => createClassByDocumentClassName(doc))
          collection.immutable.Set[Any](elems: _*) // Type check
      }
      //Sets: mutable
      case typ if
      //typ <:< typeOf[collection.mutable.HashSet[_]] ||
      typ <:< typeOf[collection.mutable.Set[_]] => {
        value: Any =>

          val elems: mutable.Set[Any] = value.asInstanceOf[util.Set[ODocument]].asScala.map(doc => createClassByDocumentClassName(doc))
          elems
      }
      //Sets: misc
      case typ if
      typ <:< typeOf[collection.Set[_]] => {
        value: Any =>

          val elems: Set[Any] = value.asInstanceOf[util.Set[ODocument]].asScala.map(doc => createClassByDocumentClassName(doc)).toSet
          elems
      }

      //Lists : immutable
      case typ if
      typ <:< typeOf[collection.immutable.List[_]] ||
        typ <:< typeOf[collection.immutable.Iterable[_]] ||
        typ <:< typeOf[collection.immutable.Seq[_]] ||
        typ <:< typeOf[collection.immutable.LinearSeq[_]] => {
        value: Any =>
          val elems: List[Any] = value.asInstanceOf[util.List[ODocument]].asScala.map(doc => createClassByDocumentClassName(doc)).toList
          collection.immutable.List(elems: _*)
      }
      //Lists : mutable
      case typ if
      typ <:< typeOf[collection.mutable.Buffer[_]] ||
        typ <:< typeOf[collection.mutable.IndexedSeq[_]] ||
        typ <:< typeOf[collection.mutable.Seq[_]] ||
        typ <:< typeOf[collection.mutable.Iterable[_]] => {
        value: Any =>

          // TODO: Result an ArrayBuffer?
          val elems: mutable.Buffer[Any] = value.asInstanceOf[util.List[ODocument]].asScala.map(doc => createClassByDocumentClassName(doc))
          elems
      }
      //Lists : misc
      case typ if
      typ <:< typeOf[collection.IndexedSeq[_]] ||
        typ <:< typeOf[collection.Seq[_]] => {
        value: Any =>

          val elems = value.asInstanceOf[util.List[ODocument]].asScala.map(doc => createClassByDocumentClassName(doc))
          elems.asInstanceOf[collection.IndexedSeq[_]]
      }

      case typ if typ.typeSymbol.isClass => {
        // Ensure there's readers for component types
        this.addType(typ)

        { value: Any => this.createClassByDocumentClassName(value.asInstanceOf[ODocument]) }
      }
      case _: Any => {
        throw new Exception(s"Unhandled read type ${typ}")
      }
    }
  }

}
