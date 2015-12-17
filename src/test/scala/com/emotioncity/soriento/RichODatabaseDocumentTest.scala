package com.emotioncity.soriento

import com.emotioncity.soriento.RichODatabaseDocumentImpl._
import com.emotioncity.soriento.RichODocumentImpl._
import com.emotioncity.soriento.support.{OrientDbSupport, RemoteOrientDbSupport}
import com.emotioncity.soriento.testmodels.ClassWithOptionalRid
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatest.{BeforeAndAfter, FunSuite, Inside, Matchers}

import scala.concurrent.{Future, blocking}
import scala.concurrent.ExecutionContext.Implicits.global


/**
  * Created by stream on 02.12.15.
  */
class RichODatabaseDocumentTest extends FunSuite
with Matchers with BeforeAndAfter with Inside with ScalaFutures with Dsl with ODb {

  implicit val defaultPatience =
    PatienceConfig(timeout = Span(5, Seconds), interval = Span(500, Millis))

  import ODocumentReader._

  test("async query by sql list of models") {
    createOClass[ClassWithOptionalRid]
    val mayBeModel1 = ClassWithOptionalRid(name = "My model1").save.as[ClassWithOptionalRid]
    val mayBeModel2 = ClassWithOptionalRid(name = "My model2").save.as[ClassWithOptionalRid]
    val mayBeModel3 = ClassWithOptionalRid(name = "My model3").save.as[ClassWithOptionalRid]
    mayBeModel1 shouldBe defined
    val model1 = mayBeModel1.get
    inside(model1) { case ClassWithOptionalRid(rid, name) =>
      rid shouldBe defined
      name should equal("My model1")
    }
    mayBeModel2 shouldBe defined
    val model2 = mayBeModel2.get
    inside(model2) { case ClassWithOptionalRid(rid, name) =>
      rid shouldBe defined
      name should equal("My model2")
    }
    mayBeModel3 shouldBe defined
    val model3 = mayBeModel3.get
    inside(model3) { case ClassWithOptionalRid(rid, name) =>
      rid shouldBe defined
      name should equal("My model3")
    }
    val savedModelsFuture = orientDb.asyncQueryBySql[ClassWithOptionalRid]("select from ClassWithOptionalRid")
    whenReady(savedModelsFuture) { savedModels =>
      savedModels shouldBe a[List[_]]
      savedModels should have size 3
    }
    dropOClass[ClassWithOptionalRid]
  }

  test("call save method from implicit") {
    val model = ClassWithOptionalRid(None, name = "name")
    val savedModel = orientDb.saveAs[ClassWithOptionalRid](model)
    println(s"Saved model: $savedModel")
  }

}
