package com.emotioncity.soriento

import java.util.{List => JList}

import com.orientechnologies.orient.core.record.impl.ODocument
import scala.reflect.runtime.universe._
import scala.util.Try

object ODocumentReader {

  implicit def createReader[T](implicit tag: TypeTag[T]): ODocumentReader[T] = {
    new ODocumentReader[T] {
      override def read(oDocument: ODocument): T = {
        ReflectionUtils.createCaseClass[T](oDocument)(tag)
      }
    }
  }
}


trait ODocumentReader[T] extends OReader[ODocument, T]

trait OReader[B <: ODocument, T] {
  /**
   * Reads a ODocument value and produce an instance of `T`.
   *
   * This method may throw exceptions at runtime.
   * If used outside a reader, one should consider `readTry(oDocument: B): Try[T]` or `readOpt(oDocument: B): Option[T]`.
   */
  def read(oDocument: B): T

  def readCollection(oDocumentCollection: List[ODocument]): T = ???

  /** Tries to produce an instance of `T` from the `oDocument` value, returns `None` if an error occurred. */
  def readOpt(oDocument: B): Option[T] = readTry(oDocument).toOption

  /** Tries to produce an instance of `T` from the `oDocument` value. */
  def readTry(oDocument: B): Try[T] = Try(read(oDocument))

}