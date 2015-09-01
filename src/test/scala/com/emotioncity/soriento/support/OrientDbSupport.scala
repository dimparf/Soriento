package com.emotioncity.soriento.support

import com.orientechnologies.orient.core.db.OPartitionedDatabasePool

/**
 * Created by b0c1 on 2015.08.12..
 */
/*
  * Copyright (c) 2014 Dmitriy Parenskiy aka stream (dimparf@gmail.com)
  */
trait OrientDbSupport {
  val oDatabaseDocumentPool = new OPartitionedDatabasePool("memory:Test"+Thread.currentThread().getId, "admin", "admin").setAutoCreate(true)
  implicit val orientDb = oDatabaseDocumentPool.acquire()
}
