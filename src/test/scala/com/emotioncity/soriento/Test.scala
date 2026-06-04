package com.emotioncity.soriento

import com.emotioncity.soriento.testmodels.{CCRecursive, Complex, Simple}
import com.orientechnologies.orient.core.db.ODatabaseRecordThreadLocal
import com.orientechnologies.orient.core.metadata.schema.OType
import com.orientechnologies.orient.core.record.impl.ODocument
import java.util.{ArrayList => JAList}

/**
 * Created by stream on 14.07.15.
 */
object TestApp extends App with Dsl with ODb {
  val oClass = createOClass[CCRecursive]
  println(s"OClass: $oClass")
  val doc = new ODocument("CCRecursive")
  doc.field("sField", "string").field("rList", new JAList(), OType.LINKLIST).save()
  println(s"Document: $doc")
}
