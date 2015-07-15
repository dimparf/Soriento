package com.emotioncity.soriento

import com.emotioncity.soriento.annotations.{Embedded, LinkSet}

import scala.reflect.runtime.universe._
import com.emotioncity.soriento.ReflectionUtils._

/**
 * Created by stream on 14.07.15.
 */
case class E(field: String)

case class TestTest(@Embedded tt: E, @javax.persistence.Id @LinkSet lt: Int, withoutAnn: String)

object TestApp extends App {
  /*  val typeOfTestTest = typeOf[TestTest]
    val companionSymbol = typeOfTestTest.typeSymbol.companion
    val applyAnno = companionSymbol
      .typeSignature
      .members
      .collectFirst { case method: MethodSymbol if method.name.toString == "apply" =>
      method.paramLists.head.collect { case p if p.annotations.exists(a => a.tree.tpe == typeOf[Embedded]) =>
        p.name.toString
      }
    }.get
    println(applyAnno)*/
 /* def fieldsWithAnnotations[T: TypeTag] = {
    val typeOfClazz = typeOf[T]
    val companionSymbol = typeOfClazz.typeSymbol.companion
    companionSymbol
      .typeSignature
      .members
      .collectFirst { case method: MethodSymbol if method.name.toString == "apply" =>
      method.paramLists.head.map(p => p.name.toString -> p.annotations)
    }
  }*/


  val typeOfTest = getTypeForClass(TestTest(E("ff"), 12, "tttt").getClass)
  println(fieldsWithAnnotations(typeOfTest))

}
