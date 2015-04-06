package com.emotioncity.soriento

import scala.reflect.runtime.universe.TypeTag

/**
 * Created by stream on 05.04.15.
 */
case class SRet[T](value: T)(implicit tag: TypeTag[T]) {
  val tpe = tag.getClass
}
