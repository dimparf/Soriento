package com.emotioncity.soriento.loadbyname


import com.emotioncity.soriento.{Dsl, EnumReflector, ODb, ODocumentReader, ReflectionUtils}
import com.orientechnologies.orient.core.metadata.schema.OType
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx
import com.orientechnologies.orient.core.id.ORID
import ODBUtil._
import com.emotioncity.soriento.testmodels._
import com.orientechnologies.orient.core.metadata.schema.OClass
import com.orientechnologies.orient.core.record.impl.ODocument
import com.orientechnologies.orient.core.sql.query.OSQLSynchQuery
import org.apache.commons.collections.EnumerationUtils
import org.scalatest.{BeforeAndAfter, FunSuite, Matchers}
import polymorphicmodels.LoginEvent

import scala.collection.JavaConverters._


class PolymorphicLoadByNameTest extends FunSuite with Matchers with BeforeAndAfter with Dsl {

  test("Duplicate register class that exists in existing DB") {
    withDropDB(makeTestDB()) { implicit db: ODatabaseDocumentTx =>


      import polymorphicmodels._

    {
      val odb = new ODb {}
      odb.createOClass[LoginEvent]
      odb.createOClass[ViewEvent]
      odb.createOClass[LoginEvent] // Test duplicate registration
      odb.createOClass[ViewEvent]
    }

    {
      // Check that ODb object's cache does not mask error.
      val odb = new ODb {}
      odb.createOClass[LoginEvent]
      odb.createOClass[ViewEvent]
      odb.createOClass[LoginEvent] // Test duplicate registration
      odb.createOClass[ViewEvent]
    }

    }
  }


  test("Late binding of reader") {

    var registered = false

    def registerReader(classname:String, registry:ClassNameReadersRegistry): Boolean ={
      classname match {
        case "LoginEvent" =>
          registered = true
          registry.add[LoginEvent]
          true
        case _=> false
      }
    }

    withDropDB(makeTestDB()) { implicit db: ODatabaseDocumentTx =>
      val odb = new ODb {}
      odb.createOClass[LoginEvent]

      val obj = LoginEvent(1000)
      db.save(obj)

      implicit val documentReader = ClassNameReadersRegistry(onMissingClassRead = registerReader)
      import AnyRichODatabaseDocumentImpl._

      documentReader.readers.contains("LoginEvent") should be(false)
      val traces: Seq[LoginEvent] = db.queryAnyBySql[LoginEvent]("select * from LoginEvent;")(documentReader)
      registered should be (true)
      documentReader.readers.contains("LoginEvent") should be(true)

      (traces.size) should be(1)
      (obj == traces(0)) should be(true)
    }
  }

  test("Polymorphic") {
    withDropDB(makeTestDB()) { implicit db: ODatabaseDocumentTx =>

      val odb = new ODb {}

      import polymorphicmodels._

      // Test polymorphic.

      val teSchema = List(odb.createOClass[TraceElementWithID]).asJava
      odb.createOClass[TraceElementViewEvent].setSuperClasses(teSchema)
      odb.createOClass[TraceElementLoginEvent].setSuperClasses(teSchema)
      odb.createOClass[UserTrace]
      odb.createOClass[LoginEvent]
      odb.createOClass[ViewEvent]

      odb.createOClass[LoginEvent] // Test duplicate registration
      odb.createOClass[ViewEvent]

      val userTrace = UserTrace(
        traceElements = List(
          TraceElementLoginEvent(date = 123, data = LoginEvent(1000)),
          TraceElementViewEvent(date = 124, data = ViewEvent(1000, 99))))

      db.save(userTrace)

      implicit val typeReaders = ClassNameReadersRegistry()

      typeReaders.add[LoginEvent]
      typeReaders.add[ViewEvent]
      typeReaders.add[TraceElementLoginEvent]
      typeReaders.add[TraceElementViewEvent]
      val rdr = typeReaders.add[UserTrace]

      typeReaders.add[TraceElementViewEvent] // Test duplicate registration
      (rdr eq typeReaders.add[UserTrace]) should be(true)

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


  test("Exception on unconstructable") {

    implicit val typeReaders = ClassNameReadersRegistry()
    typeReaders.add[AllTypeFields]

    val dsl = new Dsl {}

    try {
      val doc = dsl.productToDocument(AllTypeFields())
      doc.field("i", 2.0) // double were int is expected
      doc.field("s", null.asInstanceOf[String]) // exception should not fail to report with null parameters.
      val obj2 = typeReaders.read(doc)
    }
    catch {
      case e: DocumentReadConstructException => {
        println(e.getMessage)
      }
    }
  }

  test("Allow null object field") {
    implicit val typeReaders = ClassNameReadersRegistry()
    typeReaders.add[AllTypeFields]
    val dsl = new Dsl {}
    val doc = dsl.productToDocument(AllTypeFields())
    doc.field("string", null.asInstanceOf[String])  // Should be able to load null into an object field.
  }


  test("Exception on null primitive") {
    // Deserializing null and putting it into a Double, Int, Short, ... as 0 is probably a mistake.
    // Should throw and exception

    implicit val typeReaders = ClassNameReadersRegistry()
    typeReaders.add[AllTypeFields]

    val dsl = new Dsl {}

    try {
      val doc = dsl.productToDocument(AllTypeFields())
      doc.field("i", null.asInstanceOf[String])
      val obj2 = typeReaders.read(doc)
    }
    catch {
      case e: DocumentReadConstructException => {
        println(e.getMessage)
      }
    }
  }

  test("All type fields") {
    withDropDB(makeTestDB()) { implicit db: ODatabaseDocumentTx =>
      val odb = new ODb {}
      val oclass: OClass = odb.createOClass[AllTypeFields]
      odb.createOClass[Thing]

      // Default mapping for enums is to INTEGER
      oclass.getProperty("e").getType() should be(OType.INTEGER)
      oclass.getProperty("eOpt").getType() should be(OType.INTEGER)

      oclass.getProperty("objArray").getType() should be(OType.EMBEDDEDLIST)
      oclass.getProperty("objISeq").getType() should be(OType.EMBEDDEDLIST)
      oclass.getProperty("objISeq").getLinkedClass().toString  should be("Thing")

      val obj = AllTypeFields(
        e = WeekdayEnum.FRI,
        eOpt = Some(WeekdayEnum.THU))

      db.save(obj)


    {
      val results: java.util.List[ODocument] = db.query(new OSQLSynchQuery[ODocument]("select * from AllTypeFields;"))
      val doc = results.get(0)
      doc.fieldType("e") should be(OType.INTEGER)
      doc.fieldType("eOpt") should be(OType.INTEGER)
    }

    {

      implicit val typeReaders = ClassNameReadersRegistry()
      typeReaders.add[AllTypeFields]

      import AnyRichODatabaseDocumentImpl._

      val objs: Seq[AllTypeFields] = db.queryAnyBySql[AllTypeFields]("select * from AllTypeFields;")
      objs.size should be(1)
      val converted = objs(0)
      obj.id.isDefined should be (false)
      converted.id.isDefined should be (true)
      converted.objISeq.isInstanceOf[IndexedSeq[_]] should be(true)
      converted.objISeq(1).isInstanceOf[Thing] should be(true)
      (obj eq converted) should be(false)
      (converted.e eq WeekdayEnum.FRI) should be(true)
      (converted.e == obj.e) should be(true)
      (converted.e eq obj.e) should be(true)
      (converted eq obj) should be(false)
      // Arrays don't behave well with ==
      // Also, the loaded object has a id, the original does not
      (converted.withNullIDs().copy(objArray=null) == obj.copy(objArray=null)) should be(true)

      // Compare array contens
      for( (a,b) <- converted.objArray.zip(obj.objArray) ){
        (a==b) should be(true)
      }
    }
    }
  }


}

