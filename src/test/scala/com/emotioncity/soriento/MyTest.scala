package com.emotioncity.soriento

import org.scalatest.{BeforeAndAfter, FunSuite, Inside, Matchers}

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx
import com.emotioncity.soriento.testmodels._

class MyTest extends FunSuite with Matchers with BeforeAndAfter with ODb with Dsl {

  import com.emotioncity.soriento.ODocumentReader._
  import com.emotioncity.soriento.RichODatabaseDocumentImpl._


  test("serializeTest5") {

    val db: ODatabaseDocumentTx = new ODatabaseDocumentTx("memory:jsondb")

    createOClass[Blog]
    createOClass[Record]

    db.create()


    val blog = Blog(author = "Arnold", message = Record("Agrh!"))
    db.save(blog)
    db.save(blog)
    val blogs: List[Blog] = db.queryBySql[Blog]("select from blog")
    println(blogs)
    println(blogs.size)

    val blog2 = Blog(author = "Arnold2", message = Record("Agrh2!"))
    db.save(blog2) // ** DB is closed here
    val blogs2: List[Blog] = db.queryBySql[Blog]("select from blog")
    println(blogs2)

    db.drop()
  }
}