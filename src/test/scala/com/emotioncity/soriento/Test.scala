package com.emotioncity.soriento

import com.orientechnologies.orient.core.db.ODatabaseRecordThreadLocal

/**
 * Created by stream on 14.07.15.
 */
object TestApp extends App with Dsl with ODb {
  ODatabaseRecordThreadLocal.INSTANCE.set(orientDb)
  createOClass[Complex]
  val complex = Complex(12, Simple("test"), "testString", List(Simple("test2"), Simple("test3")))
  val complexDoc = productToDocument(complex)
  println("Generated complexDoc: " + complexDoc)
  complexDoc.save()
}
