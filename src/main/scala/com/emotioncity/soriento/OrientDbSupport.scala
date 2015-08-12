package com.emotioncity.soriento

import com.orientechnologies.orient.core.db.OPartitionedDatabasePool
import com.orientechnologies.orient.core.db.document.{ODatabaseDocument, ODatabaseDocumentTx}
import com.tinkerpop.blueprints.impls.orient.OrientGraph

/*
 * Copyright (c) 2014 Dmitriy Parenskiy aka stream (dimparf@gmail.com)
 */
trait OrientDbSupport {
  val oDatabaseDocumentPool = new OPartitionedDatabasePool("memory:Test", "admin", "admin").setAutoCreate(true)
  implicit val orientDb = oDatabaseDocumentPool.acquire()
}



