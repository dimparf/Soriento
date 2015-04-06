package com.emotioncity.soriento

import com.orientechnologies.orient.core.record.impl.ODocument

import java.util.{List => JList}
import scala.collection.generic.CanBuildFrom
import scala.util.Try
import scala.collection.JavaConversions._

/**
 * Created by stream on 30.03.15.
 */
trait ODocumentReader[T] extends OReader[ODocument, T]

trait OReader[B <: ODocument, T] {
  /**
   * Reads a ODocument value and produce an instance of `T`.
   *
   * This method may throw exceptions at runtime.
   * If used outside a reader, one should consider `readTry(oDocument: B): Try[T]` or `readOpt(oDocument: B): Option[T]`.
   */
  def read(oDocument: B): T

  /** Tries to produce an instance of `T` from the `oDocument` value, returns `None` if an error occurred. */
  def readOpt(oDocument: B): Option[T] = readTry(oDocument).toOption

  /** Tries to produce an instance of `T` from the `oDocument` value. */
  def readTry(oDocument: B): Try[T] = Try(read(oDocument))

}
