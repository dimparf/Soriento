package com.emotioncity.soriento

import com.orientechnologies.orient.core.db.OPartitionedDatabasePool
import com.orientechnologies.orient.core.db.document.{ODatabaseDocument, ODatabaseDocumentTx}
import com.tinkerpop.blueprints.impls.orient.OrientGraph

/*
 * Copyright (c) 2014 Dmitriy Parenskiy aka stream (dimparf@gmail.com)
 */
trait OrientDbSupport {
  val oDatabaseDocumentPool = new OPartitionedDatabasePool("remote:localhost/emotiongraph", "root", "varlogr3_")
  implicit val orientDb = oDatabaseDocumentPool.acquire()
}



