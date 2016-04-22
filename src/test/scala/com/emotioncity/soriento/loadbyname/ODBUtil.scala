package com.emotioncity.soriento.loadbyname

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

}
