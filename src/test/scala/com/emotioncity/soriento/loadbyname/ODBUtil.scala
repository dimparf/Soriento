package com.emotioncity.soriento.loadbyname

import com.orientechnologies.orient.client.remote.OServerAdmin
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx
import com.orientechnologies.orient.core.metadata.schema.OClass
import com.orientechnologies.orient.core.record.impl.ODocument
import com.orientechnologies.orient.core.sql.query.OSQLSynchQuery
import scala.collection.JavaConverters._

/**
  * Created by sir on 4/22/16.
  */
object ODBUtil{

  def printOClass(clz: OClass): Unit = {
    println(s"Class ${clz}")
    println("  Properties")
    for (p <- clz.declaredProperties.asScala) {
      println(s"   ${p}")
    }
    println("  Superclasses")
    for (p <- clz.getAllSuperClasses.asScala) {
      println(s"   ${p}")
    }
    println("  Subclasses")
    for (p <- clz.getAllSubclasses.asScala) {
      println(s"   ${p}")
    }
  }

  def printDocument(doc: ODocument, indent: String = ""): Unit = {
    println(s"${indent}{")

    println(s"${indent}  @class=${doc.getClassName}")
    println(s"${indent}  @rid=${doc.getIdentity}")
    println(s"${indent}  @version=${doc.getVersion}")
    println(s"${indent}  @schema=${doc.getSchemaClass}")

    for (n <- doc.fieldNames()) {
      val value = doc.field[Any](n)
      if (value == null)
        println(s"${indent}  ${n}: ${value}")
      else {
        value match {
          case value: ODocument => {
            print(s"${indent}  ${n}: ")
            printDocument(value, indent + "  ")
          }
          case value: Any => println(s"${indent}  ${n}: ${value}")
        }
      }
    }
    println(s"${indent}}")
  }

  def dumpAllDocuments(db: ODatabaseDocumentTx): Unit = {
    var schema = db.getMetadata().getSchema()
    for (clz: OClass <- schema.getClasses.asScala) {
      println(s"-------------- ${clz}")
      val result = db.command(new OSQLSynchQuery[ODocument](s"select * from ${clz};")).execute[java.util.List[ODocument]]().asScala
      for (doc <- result) {
        println(doc.toJSON)
        printDocument(doc)
      }
    }
  }


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


}
