package com.emotioncity.soriento.loadbyname

import com.emotioncity.soriento.{Dsl, ODb}
import com.orientechnologies.orient.core.db.document.{ODatabaseDocument, ODatabaseDocumentTx}
import org.scalatest.{BeforeAndAfter, FunSuite, Matchers}
import scala.collection.JavaConverters._
import com.emotioncity.soriento.loadbyname._


package models{
  /**
    * For testing loading polymorphic list Seq[TraceElement]
    */
  trait TraceElement {
    val date: Long
  }

  case class LoginEvent(val userID: Int)


  case class TraceElementLoginEvent(val date: Long, val data: LoginEvent) extends TraceElement

  case class ViewEvent(val userID: Int, val itemID: Int)


  case class TraceElementViewEvent(val date: Long, val data: ViewEvent) extends TraceElement

  /**
    * For testing loading polymorphic list Seq[TraceElement] of generics.
    * Fails because oCreateClass saves "GenericTrace" as DB Type. Needs to be
    * "GenericTrace[LoginEvent]" or "GenericTrace_LoginEvent"
    * in order to name a concrete type that can be deserialized?
    * NB. Runtime type erasure may make this a non-issue.
    */
  trait TraceElement3 {
    val date: Long
  }

  case class GenericTrace[T](val date: Long, val data: T) extends TraceElement3

  /**
    * For polymorphic deserialization in class hierarchies.
    * Currently date is not deserialized if TraceElement2LoginEvent is saved.
    */
  class TraceElement2(val date: Long)

  case class TraceElement2LoginEvent(override val date: Long, val userID: Int) extends TraceElement2(date)



  case class TraceElement2ViewEvent(override val date: Long, val userID: Int, val itemID: Int) extends TraceElement2(date)

}

class PolyMorphicLoadByNameTest extends FunSuite with Matchers with BeforeAndAfter with ODb with Dsl {


  test("Polymorphic test") {
    import models._


    implicit val db: ODatabaseDocumentTx = new ODatabaseDocumentTx("memory:jsondb")

    db.create()

    val teSchema = List(db.getMetadata().getSchema().createClass("TraceElement")).asJava

    {
      createOClass[TraceElementViewEvent].setSuperClasses(teSchema)
      createOClass[TraceElementLoginEvent].setSuperClasses(teSchema)

      db.save(new TraceElementLoginEvent(123, LoginEvent(1000)))
      db.save(new TraceElementViewEvent(124, ViewEvent(1000, 99)))
    }

    if (false) {

      val teSchema2 = List(db.getMetadata().getSchema().createClass("TraceElement2")).asJava
      createOClass[TraceElement2ViewEvent].setSuperClasses(teSchema)
      createOClass[TraceElement2LoginEvent].setSuperClasses(teSchema)

      db.save(new TraceElement2LoginEvent(123, 1000))
      db.save(new TraceElement2ViewEvent(124, 1000, 99))
    }

    if (false) {
      // Both generic variants fail:

      // Type erase fails createOClass
      // Seems like this could be made to work.
      createOClass[GenericTrace[_]].setSuperClasses(teSchema)

      // Fails at load
      //      createOClass(classTag[GenericTrace[LoginEvent]], db).setSuperClasses(teSchema)
      //      createOClass(classTag[GenericTrace[LoginEvent]], db).setSuperClasses(teSchema)

      // BUG/Limitation
      db.save(new GenericTrace(123, LoginEvent(1000)))
      db.save(new GenericTrace(123, ViewEvent(1000, 99)))
    }




    val typeReaders = ClassNameReadersRegistry()

    typeReaders.add[TraceElement2LoginEvent]
    typeReaders.add[TraceElement2ViewEvent]
    typeReaders.add[TraceElementLoginEvent]
    typeReaders.add[TraceElementViewEvent]
    typeReaders.add[GenericTrace[_]]
    //    typeReaders.add[GenericTrace[LoginEvent]]
    //    typeReaders.add[GenericTrace[ViewEvent]]

    implicit val reader = new ByClassNameODocumentReader(typeReaders)
    import AnyRichODatabaseDocumentImpl._

    {
      // Retreives a polymorphic list
      val blogs: List[TraceElement] = db.queryAnyBySql[TraceElement]("select * from TraceElement;")
      println(blogs)
      blogs.size should be(2)
    }

    if (false) {
      // Fails
      val blogs: List[TraceElement2] = db.queryAnyBySql[TraceElement2]("select * from TraceElement2;")
      println(blogs)
      blogs.size should be(2)
    }

    if (false) {
      // Fails
      // Retreives a polymorphic list of generics.
      val blogs: List[TraceElement3] = db.queryAnyBySql[TraceElement3]("select * from TraceElement3;")
      println(blogs)
      blogs.size should be(2)
    }



    db.drop()
  }

}

