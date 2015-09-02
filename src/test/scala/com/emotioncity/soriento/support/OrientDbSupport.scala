package com.emotioncity.soriento.support

import com.orientechnologies.orient.core.db.OPartitionedDatabasePool

/*
 * Copyright (c) 2014 Dmitriy Parenskiy aka stream (dimparf@gmail.com)
 */
trait OrientDbSupport {
  val oDatabaseDocumentPool = new OPartitionedDatabasePool("memory:Test" + Thread.currentThread().getId, "admin", "admin").setAutoCreate(true)
  implicit val orientDb = oDatabaseDocumentPool.acquire()
}



