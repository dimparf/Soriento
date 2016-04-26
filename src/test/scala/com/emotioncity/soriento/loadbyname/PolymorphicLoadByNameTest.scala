package com.emotioncity.soriento.loadbyname

import javax.persistence.Id

import com.emotioncity.soriento.annotations._
import com.emotioncity.soriento.{Dsl, ODb, ReflectionUtils}
import com.orientechnologies.orient.client.remote.OServerAdmin
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx
import com.orientechnologies.orient.core.id.ORID
import org.scalatest.{BeforeAndAfter, FunSuite, Matchers}

import scala.reflect.runtime.universe._
import scala.collection.JavaConverters._


package polymorphicmodels {

  import com.emotioncity.soriento.annotations._
  import com.orientechnologies.orient.core.id.ORID

  /**
    * For testing loading polymorphic list Seq[TraceElement]
    *
    * TODO:  createOClass[<trait>] doesn't add trait's fields to the schema on the DB (although it does
    * create the class).
    */
  trait TraceElement {
    val date: Int
  }

  trait TraceElementWithID extends TraceElement {
    val id: Option[ORID]
  }

  case class LoginEvent(val userID: Int)

  case class TraceElementLoginEvent(@Id override val id: Option[ORID] = None,
                                    val date: Int,
                                    @Embedded val data: LoginEvent) extends TraceElementWithID

  case class ViewEvent(val userID: Int, val itemID: Int)

  case class TraceElementViewEvent(@Id override val id: Option[ORID] = None,
                                   val date: Int,
                                   @Embedded val data: ViewEvent) extends TraceElementWithID

  case class UserTrace(@Id val userID: Option[ORID] = None,
                       // Linked items will get an ID when unserialized.
                       // NB. This list is polymorphic
                       @LinkList val traceElements: List[TraceElementWithID]
                      )

}

package failingmodels {

  /**
    * For testing loading polymorphic list Seq[TraceElement] of generics.
    * Fails because oCreateClass saves "GenericTrace" as DB Type. Needs to be
    * "GenericTrace[LoginEvent]" or "GenericTrace_LoginEvent"
    * in order to name a concrete type that can be deserialized?
    *
    * NB. Does runtime type erasure may make this a non-issue (should use "GenericTrace"
    * as the type name and new a GenericTrace[Any] and load-time?)
    */
  trait TraceElement3 {
    val date: Int
  }

  case class GenericTrace[T](val date: Int, val data: T) extends TraceElement3

  /**
    * For polymorphic deserialization in class hierarchies.
    * Currently date is not deserialized if TraceElement2LoginEvent is saved.
    */
  class TraceElement2(val date: Long)

  case class TraceElement2LoginEvent(override val date: Long, val userID: Int) extends TraceElement2(date)


  case class TraceElement2ViewEvent(override val date: Long, val userID: Int, val itemID: Int) extends TraceElement2(date)

}

package basicmodels {

  case class Message(content: String)

  case class Blog(author: String, @Linked message: Message)

  // or @Linked
  case class BlogWithEmbeddedMessages(author: String, @LinkList messages: List[Message])

}

package reflectmodels {

  class TraceElement3(val id: Int, val date: Long)

  case class LoginEvent3(val userID: Int)

  case class TraceElementLoginEvent3(@Id override val id: Int,
                                     override val date: Long,
                                     @Embedded val data: LoginEvent3) extends TraceElement3(id, date)

}

class PolymorphicLoadByNameTest extends FunSuite with Matchers with BeforeAndAfter with ODb with Dsl {


  def ensureNewRemoteDB(host: String,
                        dbName: String,
                        user: String = "admin",
                        pass: String = "admin",
                        remoteDBStorage: String = "plocal",
                        dbType: String = "document",
                        dropIfExists: Boolean = false): Unit = {
    val server = new OServerAdmin(s"remote:${host}/${dbName}").connect(user, pass)

    try {
      val exists = server.listDatabases.asScala.contains(dbName)
      if (exists) {
        if (dropIfExists) {
          println("DROP!")
          server.dropDatabase(dbName)
          server.createDatabase(dbName, dbType, remoteDBStorage)
        }
        // Already exists
      }
      else {
        server.createDatabase(dbName, dbType, remoteDBStorage)
      }
    }
    finally {
      server.close()
    }
  }


  def makeTestDB() = {
    if (true) {

      val db: ODatabaseDocumentTx = new ODatabaseDocumentTx("memory:testdb")
      db.create()
      db

    } else {
      // Remote DB
      // This is useful for checking that serialization actually happens.
      val dbHost = "orientdb"
      val dbName = "stu"
      val dbUser = "root"
      val dbPass = ""

      ensureNewRemoteDB(dbHost, dbName, dbUser, dbPass, remoteDBStorage = "plocal", dropIfExists = true)
      new ODatabaseDocumentTx(s"remote:${dbHost}/${dbName}").open(dbUser, dbPass)
    }
  }

  def withDropDB(dbFactory: => ODatabaseDocumentTx)(func: ODatabaseDocumentTx => Unit): Unit = {
    val db = dbFactory
    try {
      func(db)
    } finally {
      if (!db.isClosed) {
        if (!db.isActiveOnCurrentThread) {
          db.activateOnCurrentThread()
        }
        db.drop()
      }
    }
  }

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
      typeReaders.add[UserTrace]

      implicit val reader = new ByClassNameODocumentReader(typeReaders)

      import AnyRichODatabaseDocumentImpl._

      val traces: List[UserTrace] = db.queryAnyBySql[UserTrace]("select * from UserTrace;")

      println(traces)
      traces.size should be(1)

      val readTrace = traces(0)
      // eq should be false. Tests that serialization ACTUALLY happened
      (userTrace.traceElements eq readTrace.traceElements) should be(false)
      (userTrace.traceElements(0) eq readTrace.traceElements(0)) should be(false)
      (readTrace.traceElements(0).getClass) should be(classOf[TraceElementLoginEvent])
      (readTrace.traceElements(1).getClass) should be(classOf[TraceElementViewEvent])

      // DB should have set the ID
      (readTrace.userID.isDefined) should be(true)
      (readTrace.traceElements(0).id.isDefined) should be(true)
      (readTrace.traceElements(1).id.isDefined) should be(true)

      (readTrace.userID.get.isInstanceOf[ORID]) should be(true)
      (readTrace.traceElements(0).id.get.isInstanceOf[ORID]) should be(true)
      (readTrace.traceElements(1).id.get.isInstanceOf[ORID]) should be(true)

      // Remove the IDS from the object
      val userTraceWithoutIDs = readTrace.copy(
        userID = None,
        traceElements = List(
          readTrace.traceElements(0).asInstanceOf[TraceElementLoginEvent].copy(id = None),
          readTrace.traceElements(1).asInstanceOf[TraceElementViewEvent].copy(id = None))
      )
      // Object should be identical
      (userTraceWithoutIDs == userTrace) should be(true)

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

