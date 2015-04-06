package com.emotioncity.soriento

import com.orientechnologies.orient.core.record.impl.ODocument
import com.orientechnologies.orient.core.sql.query.OSQLSynchQuery

import scala.collection.JavaConversions._
import scala.reflect.runtime.universe.TypeTag
import com.orientechnologies.orient.core.db.document.ODatabaseDocument

/**
 * Created by stream on 31.03.15.
 */
object Implicits {

  implicit class RichODatabaseDocumentTx(db: ODatabaseDocument) extends ODocumentable {

    def queryBySql[T](sql: String)(implicit tag: TypeTag[T]): List[T] = {
      val results: java.util.List[ODocument] = db.query(new OSQLSynchQuery[ODocument](sql))
      results.toList.map(document => fromODocument[T](document))
    }

    def queryBySql(sql: String): List[ODocument] = {
      val results: java.util.List[ODocument] = db.query(new OSQLSynchQuery[ODocument](sql))
      results.toList
    }

    def queryBySqlWithReader[T](query: String)(implicit reader: ODocumentReader[T]): List[T] = {
      val results: java.util.List[ODocument] = db.query(new OSQLSynchQuery[ODocument](query))
      results.toList.map(document => reader.read(document))
    }

  }

}
