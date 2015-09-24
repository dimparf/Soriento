package com.emotioncity.soriento

import com.orientechnologies.orient.core.command.OCommandRequest
import com.orientechnologies.orient.core.db.ODatabaseRecordThreadLocal
import com.orientechnologies.orient.core.db.document.{ODatabaseDocument, ODatabaseDocumentTx}
import com.orientechnologies.orient.core.record.impl.ODocument
import com.orientechnologies.orient.core.sql.OCommandSQL
import com.orientechnologies.orient.core.sql.query.OSQLSynchQuery

import scala.collection.JavaConversions._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

/**
 * Created by stream on 31.03.15.
 */
object RichODatabaseDocumentImpl {

  implicit class RichODatabaseDocumentTx(db: ODatabaseDocument) {

    def queryDocumentsBySql(sql: String): List[ODocument] = {
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

    protected def asyncCall[T](x: ODatabaseDocumentTx => T): Future[T] = {
      val instance = ODatabaseRecordThreadLocal.INSTANCE.get
      Future {
        val internalDb = instance.asInstanceOf[ODatabaseDocumentTx].copy
        try {
          x(internalDb)
        } finally {
          if (internalDb != null) {
            internalDb.close()
          }
        }
      }
    }

    def asyncQueryBySql(sql: String): Future[List[ODocument]] = asyncCall { internalDb =>
      val results: java.util.List[ODocument] = internalDb.query(new OSQLSynchQuery[ODocument](sql))
      results.toList
    }

    def asyncQueryBySql[T](query: String)(implicit reader: ODocumentReader[T]): Future[List[T]] = asyncCall { internalDb =>
      val results: java.util.List[ODocument] = internalDb.query(new OSQLSynchQuery[ODocument](query))
      results.toList.map(document => reader.read(document))
    }


  }

}
