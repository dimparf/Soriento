package com.emotioncity.soriento.support

import com.orientechnologies.orient.core.db.OPartitionedDatabasePool
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx

/*
 * Copyright (c) 2014 Dmitriy Parenskiy aka stream (dimparf@gmail.com)
 */
trait OrientDbSupport {
  implicit val orientDb: ODatabaseDocumentTx = new ODatabaseDocumentTx("memory:MyDb" + Thread.currentThread().getId).create()
}

trait RemoteOrientDbSupport {
  val remotePool = new OPartitionedDatabasePool("remote:localhost/emotiongraph", "root", "varlogr3_").setAutoCreate(true)
  implicit val orientDb = remotePool.acquire()
}
