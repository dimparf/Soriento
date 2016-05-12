package com.emotioncity.soriento.loadbyname

import com.emotioncity.soriento.annotations._
import com.emotioncity.soriento.{Dsl, ODb, ReflectionUtils}
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx
import com.orientechnologies.orient.core.id.ORID
import ODBUtil._
import com.emotioncity.soriento.testmodels._
import org.scalatest.{BeforeAndAfter, FunSuite, Matchers}
import polymorphicmodels.UserTrace

import scala.reflect.runtime.universe._
import scala.collection.JavaConverters._


class PolymorphicLoadByNameTest extends FunSuite with Matchers with BeforeAndAfter with ODb with Dsl {


  test("Polymorphic") {
    withDropDB(makeTestDB()) { implicit db: ODatabaseDocumentTx =>
      import polymorphicmodels._

      // Test polymorphic.

      val teSchema = List(createOClass[TraceElementWithID]).asJava
      createOClass[TraceElementViewEvent].setSuperClasses(teSchema)
      createOClass[TraceElementLoginEvent].setSuperClasses(teSchema)
      createOClass[UserTrace]
      createOClass[LoginEvent]
      createOClass[ViewEvent]

      createOClass[LoginEvent] // Test duplicate registration
      createOClass[ViewEvent]

      val userTrace = UserTrace(
        traceElements = List(
          TraceElementLoginEvent(date = 123, data = LoginEvent(1000)),
          TraceElementViewEvent(date = 124, data = ViewEvent(1000, 99))))

      db.save(userTrace)

      val typeReaders = ClassNameReadersRegistry()

      typeReaders.add[LoginEvent]
      typeReaders.add[ViewEvent]
      typeReaders.add[TraceElementLoginEvent]
      typeReaders.add[TraceElementViewEvent]
      val rdr = typeReaders.add[UserTrace]

      typeReaders.add[TraceElementViewEvent] // Test duplicate registration
      (rdr eq typeReaders.add[UserTrace]) should be(true)

      implicit val reader = new ByClassNameODocumentReader(typeReaders)

      import AnyRichODatabaseDocumentImpl._

      val traces: Seq[UserTrace] = db.queryAnyBySql[UserTrace]("select * from UserTrace;")

      println(traces)
      traces.size should be(1)

      val readTrace = traces(0)
      // eq should be false. Tests that serialization ACTUALLY happened
      (userTrace.traceElements eq readTrace.traceElements) should be(false)
      (userTrace.traceElements(0) eq readTrace.traceElements(0)) should be(false)
      readTrace.traceElements(0).getClass should be(classOf[TraceElementLoginEvent])
      readTrace.traceElements(1).getClass should be(classOf[TraceElementViewEvent])

      // DB should have set the ID
      readTrace.userID.isDefined should be(true)
      readTrace.traceElements(0).id.isDefined should be(true)
      readTrace.traceElements(1).id.isDefined should be(true)

      readTrace.userID.get.isInstanceOf[ORID] should be(true)
      readTrace.traceElements(0).id.get.isInstanceOf[ORID] should be(true)
      readTrace.traceElements(1).id.get.isInstanceOf[ORID] should be(true)

      // Remove the IDS from the object
      val userTraceWithoutIDs = readTrace.copy(
        userID = None,
        traceElements = List(
          readTrace.traceElements(0).asInstanceOf[TraceElementLoginEvent].copy(id = None),
          readTrace.traceElements(1).asInstanceOf[TraceElementViewEvent].copy(id = None))
      )
      // Object should be identical
      (userTraceWithoutIDs == userTrace) should be(true)


      // Can also query specific type
      val loginEvents: Seq[TraceElementLoginEvent] = db.queryAnyBySql[TraceElementLoginEvent]("select * from TraceElementLoginEvent;")
      println(loginEvents)
      loginEvents.size should be(1)
      loginEvents(0).copy(id = None) should be(userTrace.traceElements(0))
    }
  }

  test("All type fields") {
    withDropDB(makeTestDB()) { implicit db: ODatabaseDocumentTx =>

      createOClass[AllTypeFields]

      val obj = AllTypeFields(
        e = WeekdayEnum.FRI,
        eOpt = Some(WeekdayEnum.THU))

      db.save( obj)


      val typeReaders = ClassNameReadersRegistry()
      typeReaders.add[AllTypeFields]
      implicit val reader = new ByClassNameODocumentReader(typeReaders)
      import AnyRichODatabaseDocumentImpl._

      val objs: Seq[AllTypeFields] = db.queryAnyBySql[AllTypeFields]("select * from AllTypeFields;")
      objs.size should be (1)
      val converted = objs(0)
      (converted eq objs) should be(false)
      (converted.withNullIDs() eq objs) should be(false)
      (converted.withNullIDs() == obj) should be(true)
      (converted.e eq obj.e) should be(true)
    }
  }
}

