package com.emotioncity.soriento

import com.emotioncity.soriento.config.SorientoConfig
import com.orientechnologies.orient.core.command.OCommandRequest
import com.orientechnologies.orient.core.db.ODatabaseRecordThreadLocal
import com.orientechnologies.orient.core.db.document.{ODatabaseDocument, ODatabaseDocumentTx}
import com.orientechnologies.orient.core.record.impl.ODocument
import com.orientechnologies.orient.core.sql.OCommandSQL
import com.orientechnologies.orient.core.sql.query.OSQLSynchQuery

import scala.collection.JavaConversions._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Future, blocking}

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

    def command(query: String): OCommandRequest = {
      db.command[OCommandRequest](new OCommandSQL(query)).execute() //type annotation of return?
    }

    protected def asyncCall[T](x: ODatabaseDocumentTx => T)(implicit config: SorientoConfig): Future[T] = {
      val instance = ODatabaseRecordThreadLocal.INSTANCE.get
      Future {
        val internalDb: ODatabaseDocumentTx = if (config.poolIsDefined) {
          println("Use pooled connection")
          config.oDatabaseDocumentPool.get.acquire()
        } else {
          println("Create new unpooled connection")
          ODatabaseRecordThreadLocal.INSTANCE.set(instance)
          instance.asInstanceOf[ODatabaseDocumentTx].copy
        }
        try {
          blocking {
            x(internalDb)
          }
        } finally {
          if (internalDb != null) {
            internalDb.close()
          }
        }
      }
    }

    def asyncQueryBySql(sql: String)(implicit config: SorientoConfig): Future[List[ODocument]] = asyncCall { internalDb =>
      val results: java.util.List[ODocument] = internalDb.query(new OSQLSynchQuery[ODocument](sql))
      results.toList
    }

    def asyncQueryBySql[T](query: String)(implicit reader: ODocumentReader[T], config: SorientoConfig): Future[List[T]] = asyncCall { internalDb =>
      val results: java.util.List[ODocument] = internalDb.query(new OSQLSynchQuery[ODocument](query))
      results.toList.map(document => reader.read(document))
    }


  }

}
