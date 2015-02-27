package com.emotioncity.soriento

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx
import com.orientechnologies.orient.core.db.document.ODatabaseDocument
import com.orientechnologies.orient.core.record.impl.ODocument
import com.orientechnologies.orient.core.sql.query.OSQLSynchQuery

import scala.collection.JavaConversions._
import scala.reflect.runtime.universe.TypeTag

/*
 * Copyright (c) 2014 Dmitriy Parenskiy aka stream (dimparf@gmail.com)
 */
trait OrientDbSupport {

  //connect remote:localhost/emotioncity root varlogr3_
  implicit val orientDb: ODatabaseDocumentTx =
  new ODatabaseDocumentTx("remote:localhost/emotioncity").open("root", "varlogr3_")
}

object Implicits {

  implicit class RichODatabaseDocumentTx(db: ODatabaseDocument) extends ODocumentable {

    def queryBySql[T](sql: String)(implicit tag: TypeTag[T]): List[T] = {
      val results: java.util.List[ODocument] = db.query(new OSQLSynchQuery[ODocument](sql))
      results.toList.map(document => fromODocument[T](document))
    }

    def queryB(sql: String): List[ODocument] = {
      val results: java.util.List[ODocument] = db.query(new OSQLSynchQuery[ODocument](sql))
      results.toList
    }

  }

}

