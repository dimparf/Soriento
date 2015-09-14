package com.emotioncity.soriento.support

import com.orientechnologies.orient.core.db.OPartitionedDatabasePool

/*
 * Copyright (c) 2014 Dmitriy Parenskiy aka stream (dimparf@gmail.com)
 */
trait OrientDbSupport {
  val oDatabaseDocumentPool = new OPartitionedDatabasePool("memory:Test" + Thread.currentThread().getId, "admin", "admin").setAutoCreate(true)
  //val remotePool = new OPartitionedDatabasePool("remote:localhost/emotiongraph", "root", "varlogr3_")

  implicit val orientDb = oDatabaseDocumentPool.acquire()
  //implicit val orientDb = remotePool.acquire()

}



