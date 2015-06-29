package com.emotioncity.soriento

import com.orientechnologies.orient.core.command.OCommandRequest
import com.orientechnologies.orient.core.record.impl.ODocument
import com.orientechnologies.orient.core.sql.OCommandSQL
import com.orientechnologies.orient.core.sql.query.OSQLSynchQuery

import scala.collection.JavaConversions._
import com.orientechnologies.orient.core.db.document.ODatabaseDocument

/**
 * Created by stream on 31.03.15.
 */
object Implicits {

  implicit class RichODatabaseDocumentTx(db: ODatabaseDocument) {

    def queryBySql(sql: String): List[ODocument] = {
      val results: java.util.List[ODocument] = db.query(new OSQLSynchQuery[ODocument](sql))
      results.toList
    }

    def queryBySql[T](query: String)(implicit reader: ODocumentReader[T]): List[T] = {
      val results: java.util.List[ODocument] = db.query(new OSQLSynchQuery[ODocument](query))
      results.toList.map(document => reader.read(document))
    }

    def command(query: String) = {
      db.command[OCommandRequest](new OCommandSQL(query)).execute() //type annotation of return?
    }

    def queryDoc(sql: String): List[ODocument] = {
      val results: java.util.List[ODocument] = db.query(new OSQLSynchQuery[ODocument](sql))
      results.toList
    }

  }

}
