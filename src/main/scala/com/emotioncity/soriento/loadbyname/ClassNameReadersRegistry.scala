package com.emotioncity.soriento.loadbyname

import java.util

import com.emotioncity.soriento.{EnumReflector, ODocumentReader, ReflectionUtils}
import _root_.com.orientechnologies.orient.core.record.impl.ODocument
import _root_.com.orientechnologies.orient.core.id.ORID

import scala.collection.JavaConverters._
import scala.collection.{Map, mutable}
import scala.reflect.runtime.universe.{Symbol, Type, TypeTag, runtimeMirror, typeOf}

object Typedefs {
  // Extracts a scala object from a document
  type DocumentReader = (ODocument => Any)

  // Extracts a specific field from a document
  type FieldReader = (ODocument => Any)

  // Maps from document field values into the type parameters need for a class constructor.
  type ValueReader = (Any => Any)

}

import Typedefs._

class DocumentReadException(val doc: ODocument=null, val message: String = null, val cause: Exception = null) extends java.lang.IllegalArgumentException(message, cause)

/**
  * Failed providing this parametes to the constructor for this type.
  */
case class DocumentReadConstructException(override val doc: ODocument,
                                          params:Array[Any],
                                          tpe: Type,
                                          override val message: String = "Failed to construct object from document",
                                          override val cause: Exception = null) extends DocumentReadException(doc, message, cause) {
  override def getMessage: String = {
    val cparams = ReflectionUtils.constructorParams(tpe)

    val table = (0 until Math.max(cparams.size, params.size)).map { i:Int =>
      val p = params(i)

      val foundType = if (i>=params.size) "<missing>" else {
        if (params(i)==null) "<null>"
        else params(i).getClass.getSimpleName
      }

      val paramString = if (i>=params.size) "<missing>" else {
        val s = s"${params(i)}"
        s.drop(s.size-40)
      }
      val paramName = if (i>=cparams.size) "<missing>" else {
        val s = s"${cparams(i).name}"
        s.drop(s.size-15)
      }

      val expectedType = if (i>=cparams.size) "xxx" else cparams(i).typeSignature.toString
      f" ${i}%3d ${paramName}%15s ${paramString}%40s : ${foundType}%-30s   ${expectedType}\n"
    }



    s"""With:
      params: ${params.mkString(",")}
         tpe: ${tpe}
         doc: ${doc}
PARAM |         NAME |                              DOC VALUE | DOC VALUE TYPE                 | CONSTRUCTOR EXPECTED TYPE
${table.mkString("")}
${super.getMessage}
"""
  }
}

/**
  * Reads a document and emits a scala type.
  *
  * @param tpe               Just for debugging.
  * @param fieldConstructors Returns constructor fields for this type from an odocument.
  */
case class DocumentFromConstructor(val tpe: Type,
                                   var fieldConstructors: Array[FieldReader]
                                  ) extends DocumentReader {
  val constr = ReflectionUtils.constructor(tpe)

  def apply(doc: ODocument): Any = {
    // TODO: constructing an array here is still quite inefficient.
    // Can avoid this? (Pool of arrays?)

    // Ask for field values from the document
    val prms: Array[Any] = fieldConstructors.map(fc => fc(doc))

    try {
      constr(prms: _*) // invoke constructor
    } catch {
      // Breakpoint here when readings fails!
      case e:java.lang.IllegalArgumentException => throw DocumentReadConstructException(doc, prms, tpe)
    }
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
  * @param onMissingClassRead Callback to read a class
  */
case class ClassNameReadersRegistry(val classNamer: (Type => String) = ClassToNameFunctions.simple,
                                    val onMissingClassRead: (String, ClassNameReadersRegistry) => Boolean = ClassNameReadersRegistry.throwMissingClassReader
                                   ) extends ODocumentReader[Any] {
  //                               ClassToNameFunctions.underscoreTypeParameters) {

  private val mirror = runtimeMirror(getClass.getClassLoader)

  // Maps from classes to readers.
  private var _readers = collection.immutable.Map[String, DocumentReader]()
  private var _classNameToType = collection.immutable.Map[String, Type]()

  def readers = _readers



  /**
    * Convert a oDocument into a scala object.
    * A reader is looked-up based on the document classname
    *
    * @param document
    * @return
    */
  def read(document: ODocument): Any = {
    val name = document.getClassName
    _readers.get(name) match {
      case Some(reader) => reader(document)
      case None => {
        if (!onMissingClassRead(name, this)) {
          ClassNameReadersRegistry.throwMissingClassReader(name)
        }

        // Try again
        _readers.get(name) match {
          case Some(reader) => reader(document)
          case None => {
            ClassNameReadersRegistry.throwMissingClassReader(name)
            null
          }
        }
      }
    }
  }

  /**
    * Register a document reader for type T
    *
    * @param tag
    * @tparam T
    * @return
    */
  def add[T](implicit tag: TypeTag[T]): DocumentReader = addType(tag.tpe)

  def add(clz:Class[_]): DocumentReader = addType(ReflectionUtils.toType(clz))

  def addType(tpe: Type): DocumentReader = {
    val className = classNamer(tpe)

    _readers.get(className) match {
      case Some(reader) => {
        _classNameToType.get(className) match {
          case Some(existingType) => {
            if (existingType =:= tpe) reader
            else throw new IllegalArgumentException(s"name '${className}' for type ${tpe} was already registered for different type ${existingType}")
          }
          case None => reader // Was registered with an unknown type.
        }
      }
      case None => {
        addReader(className, makeDocumentReaderFromType(tpe), Some(tpe))
      }
    }
  }

  /**
    * Registers a function to deserialize some className
    *
    * @param className
    * @param tpe
    * @param documentReader
    * @return
    */
  def addReader(className: String, documentReader: => DocumentReader, tpe: Option[Type] = None): DocumentReader = {
    this.synchronized {
      _classNameToType.get(className) match {
        case Some(existingType) => throw new IllegalArgumentException(s"name '${className}' for type ${tpe} was already registered for type ${existingType}")
        case None => {

          _classNameToType += (className -> tpe.get)

          val reader = DocumentFromConstructor(tpe = tpe.get, fieldConstructors = null)

          _readers += (className -> reader)
          // Instantiating documentReader may recursively ask for the same type.
          // This late binding of fieldConstructors allows recursive definitions
          reader.fieldConstructors = documentReader.asInstanceOf[DocumentFromConstructor].fieldConstructors

          reader
        }
      }
    }
  }


  private def makeDocumentReaderFromType(tpe: Type): DocumentReader = {

    val constructor = ReflectionUtils.constructor(tpe)
    val constructorParams = constructor.symbol.paramLists.flatten

    // TODO: Add ID.

    val constructorFieldReaders = constructorParams.map(symbol => toFieldReader(symbol)).toArray

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

      case typ if EnumReflector.isEnumeration(typ) => {
        val f = EnumReflector(typ).fromID _

        { i: Any => f(i.asInstanceOf[Int]) }
      }

      // Builtin types
      case typ if typ <:< typeOf[Boolean] ||
        typ <:< typeOf[Byte] ||
        typ <:< typeOf[Int] ||
        typ <:< typeOf[Long] ||
        typ <:< typeOf[Short] ||
        typ <:< typeOf[Double] ||
        typ <:< typeOf[Float] ||
        typ <:< typeOf[Char] ||
        typ <:< typeOf[String] ||
        typ <:< typeOf[Array[Byte]] ||
        typ <:< typeOf[java.lang.Boolean] ||
        typ <:< typeOf[java.lang.Byte] ||
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
          value: Any => if (value == null) None else Some(elemReader(value))
        }
      }

      case typ if typ <:< typeOf[Map[_, _]] => getValueMapperForReadMapCollection(typ)
      case typ if (typ <:< typeOf[Iterable[_]] || typ <:< typeOf[Array[_]]) => getValueMapperForReadListCollection(typ)

      case typ if typ.typeSymbol.isClass => {

        val clzSym = typ.typeSymbol.asClass

        if (!clzSym.isAbstract) {
          // Ensure there's a reader for this field class
          this.addType(typ)
        }

        if (!clzSym.isFinal) {
          // TODO. Optimize final classes with no possible subtypes.
        }

        {
          value: Any => this.read(value.asInstanceOf[ODocument])
        }
      }
      case _: Any => unhandledType(typ)
    }
  }

  private def unhandledType(typ: Type) = throw new Exception(s"Unhandled read type ${typ}")

  private def getValueMapperForReadMapCollection(typ: Type): ValueReader = {


    val genericType = typ.typeArgs(1)
    if (ReflectionUtils.isCaseClass(genericType)) {
      addType(genericType)
    }

    typ match {
      //Maps
      //Maps: immutable
      case typ if
      typ <:< typeOf[collection.immutable.HashMap[String, _]] ||
        typ <:< typeOf[collection.immutable.Map[String, _]] => {
        value: Any =>

          val elems: collection.immutable.Map[String, Any] = value.asInstanceOf[util.Map[String, ODocument]].asScala.mapValues(v => read(v)).toMap
          elems.asInstanceOf[collection.immutable.Map[String, Any]] // Type check
      }

      //Maps: mutable
      case typ if
      typ <:< typeOf[collection.mutable.HashMap[String, _]] ||
        typ <:< typeOf[collection.mutable.Map[String, _]] => {
        value: Any =>

          val elems = value.asInstanceOf[util.Map[String, ODocument]].asScala.map(kv => (kv._1 -> read(kv._2)))
          elems.asInstanceOf[collection.mutable.HashMap[String, Any]] // Type check
      }
      //Maps: misc
      case typ if
      typ <:< typeOf[collection.Map[String, _]] => {


        {
          value: Any =>
            val elems: mutable.Map[String, Any] = value.asInstanceOf[util.Map[String, ODocument]].asScala.map(kv => (kv._1 -> read(kv._2)))
            elems
        }
      }
      case _: Any => unhandledType(typ)
    }
  }

  private def getValueMapperForReadListCollection(typ: Type): ValueReader = {

    val genericType = typ.typeArgs.head
    if (ReflectionUtils.isCaseClass(genericType)) {
      addType(genericType)
    }

    val elementReader = getValueMapperForRead(genericType)

    typ match {

      //Sets: immutable
      case typ if
      //typ <:< typeOf[collection.immutable.HashSet[_]] ||
      typ <:< typeOf[collection.immutable.Set[_]] => {

        value: Any =>
          val elems: Seq[Any] = value.asInstanceOf[util.Set[Any]].asScala.toSeq.map(doc => elementReader(doc))
          collection.immutable.Set[Any](elems: _*) // Type check
      }
      //Sets: mutable
      case typ if
      //typ <:< typeOf[collection.mutable.HashSet[_]] ||
      typ <:< typeOf[collection.mutable.Set[_]] => {

        value: Any =>
          val elems: mutable.Set[Any] = value.asInstanceOf[util.Set[Any]].asScala.map(doc => elementReader(doc))
          elems
      }
      //Sets: misc
      case typ if typ <:< typeOf[collection.Set[_]] => {

        {
          value: Any =>
            val elems: Set[Any] = value.asInstanceOf[util.Set[Any]].asScala.map(doc => elementReader(doc)).toSet
            elems
        }
      }

      //Lists : misc
      case typ if typ <:< typeOf[Array[_]] => {
            val clz = ReflectionUtils.toJavaClass(genericType)

        {
          value: Any =>
            val list = value.asInstanceOf[util.List[Any]]
            val result:Array[Any] = java.lang.reflect.Array.newInstance(clz, list.size()).asInstanceOf[Array[Any]]
            var i = -1
            for (elem <- list.iterator().asScala) {
              i += 1
              result(i) = elementReader(elem)
            }
            result
        }
      }

      //Lists : immutable
      case typ if typ <:< typeOf[collection.immutable.List[_]] ||
        typ <:< typeOf[collection.immutable.Iterable[_]] ||
        typ <:< typeOf[collection.immutable.Seq[_]] ||
        typ <:< typeOf[collection.immutable.LinearSeq[_]] => { value: Any =>
            val elems: List[Any] = value.asInstanceOf[util.List[_]].asScala.map(elem => elementReader(elem)).toList
            collection.immutable.List(elems: _*)
        }


      case typ if
        typ <:< typeOf[collection.IndexedSeq[_]] ||
        typ <:< typeOf[collection.Seq[_]] ||
        typ <:< typeOf[collection.mutable.Buffer[_]] ||
        typ <:< typeOf[collection.mutable.Buffer[_]] ||
        typ <:< typeOf[collection.mutable.IndexedSeq[_]] ||
        typ <:< typeOf[collection.mutable.Seq[_]] ||
        typ <:< typeOf[collection.mutable.Iterable[_]] => { value: Any =>

          val list = value.asInstanceOf[util.List[_]]
          val result = new collection.mutable.ArrayBuffer[Any](list.size())
          for (elem <- list.iterator().asScala) {
            result += elementReader(elem)
          }
          result
      }

      case _: Any => unhandledType(typ)
    }
  }
}


object ClassNameReadersRegistry {
  def throwMissingClassReader(classname: String, registry: ClassNameReadersRegistry=null): Boolean = {
    throw new Exception(s"Document with @class '${classname}' has no reader")
    false
  }
}
