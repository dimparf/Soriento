package com.emotioncity.soriento.loadbyname

import com.emotioncity.soriento.ODocumentReader
import com.orientechnologies.orient.core.db.document.ODatabaseDocument
import com.orientechnologies.orient.core.record.impl.ODocument
import com.orientechnologies.orient.core.sql.query.OSQLSynchQuery
import scala.collection.JavaConverters._

object AnyRichODatabaseDocumentImpl {

  import com.emotioncity.soriento.RichODatabaseDocumentImpl

  /**
    * RichODatabaseDocumentTx with a T<:Any constraint.
    * @param db
    */
  implicit class AnyRichODatabaseDocumentTx(db: ODatabaseDocument) extends RichODatabaseDocumentImpl.RichODatabaseDocumentTx(db) {

    /**
      * Deserializes any object from the query result using the name provided in the @class field of the document.
      *
      * @param query
      * @param reader ODocumentReader of Any type.
      * @tparam T I expect all returned elements to be a subtype of this.
      * @return
      */
    def queryAnyBySql[T <: Any](query: String)(implicit reader: ODocumentReader[Any]): List[T] = blockingCall { db =>
      val results: java.util.List[ODocument] = db.query(new OSQLSynchQuery[ODocument](query))
      results.asScala.toList.map(document => reader.read(document)).asInstanceOf[List[T]]
      // list.map(e => e.asInstanceOf[T] )  // Validate elements
    }
  }

}
