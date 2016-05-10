package com.emotioncity.soriento.loadbyname

import com.emotioncity.soriento.annotations._
import com.emotioncity.soriento.{Dsl, ODb, ReflectionUtils}
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx
import com.orientechnologies.orient.core.id.ORID
import ODBUtil._
import org.scalatest.{BeforeAndAfter, FunSuite, Matchers}

import scala.reflect.runtime.universe._
import scala.collection.JavaConverters._


class PolymorphicLoadByNameTest extends FunSuite with Matchers with BeforeAndAfter with ODb with Dsl {

  test("Read write Basics") {
    withDropDB(makeTestDB()) { implicit db: ODatabaseDocumentTx =>

      import basicmodels._
      import com.emotioncity.soriento.RichODatabaseDocumentImpl._

      createOClass[Message]
      createOClass[BlogWithEmbeddedMessages]

      val blogWithEmbeddedMessages = BlogWithEmbeddedMessages("John", List(Message("Hi"), Message("New blog note")))

      db.save(blogWithEmbeddedMessages)
      db.save(blogWithEmbeddedMessages)


      //println("BlogWithEmbeddedMessages--------")
      val result = db.queryBySql[BlogWithEmbeddedMessages]("select * from BlogWithEmbeddedMessages;")
      //result.foreach(println(_))

      (blogWithEmbeddedMessages == result(0)) should be(true)
      (blogWithEmbeddedMessages.messages(0) eq result(0).messages(0)) should be(false)

      //println("Message--------")
      val messages = db.queryBySql[Message]("select * from Message;")
      //      messages.foreach(println(_))
      (messages.length) should be(4)
    }
  }

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

      createOClass[LoginEvent]  // Test duplicate registration
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

      val traces: List[UserTrace] = db.queryAnyBySql[UserTrace]("select * from UserTrace;")

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
      val loginEvents: List[TraceElementLoginEvent] = db.queryAnyBySql[TraceElementLoginEvent]("select * from TraceElementLoginEvent;")
      println(loginEvents)
      loginEvents.size should be(1)
      loginEvents(0).copy(id=None) should be(userTrace.traceElements(0))
    }
  }


  test("Reflect test") {
    withDropDB(makeTestDB()) { implicit db: ODatabaseDocumentTx =>

      // TODO. Update this test to refelect new implementation in Dsl.scala
      // (other tests are exercising this quite well)

      import reflectmodels._

      val te = TraceElementLoginEvent3(123, 12345, LoginEvent3(1000))



      // clz.getDeclaredFields erases overridden values
      def printDeclaredFields(clz: Class[_]): Unit = {
        for (e <- clz.getDeclaredFields.toList) {
          println(s"  F ${e.getType.isLocalClass}   -- isLocal:${e.getType.isLocalClass}  -- ${e}")
        }
        //      val c: Class[_] = clz.getSuperclass
        //      if (c != null) printDeclaredFields(c)
      }

      printDeclaredFields(te.getClass)

      for (e <- te.productIterator) println(s"VAL ${e}")



      for (e <- ReflectionUtils.caseAccessorsFromObject(te)) {
        //      for (e <- caseAccessorsT[TraceElementLoginEvent3]) {
        println(s"  A ${e} ${e.returnType}   E:${ReflectionUtils.hasAnnotation(e, typeOf[Embedded])}  I:${ReflectionUtils.isId(e)}")
      }


      val ctor = ReflectionUtils.constructor(typeOf[TraceElementLoginEvent3])
      val cParams: List[Symbol] = ctor.symbol.paramLists.flatten

      val x: String = cParams(0).name.decodedName.toString

      for (e <- cParams) {
        println(s"  C ${e} NAME(${e.name.decodedName.toString})  ${ReflectionUtils.getOType(e)} E:${ReflectionUtils.hasAnnotation(e, typeOf[Embedded])}  I:${ReflectionUtils.isId(e)}")
      }
    }
  }

}

