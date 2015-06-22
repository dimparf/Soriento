package com.emotioncity.soriento

import com.orientechnologies.orient.core.db.OPartitionedDatabasePool
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx
import com.tinkerpop.blueprints.impls.orient.OrientGraph

/*
 * Copyright (c) 2014 Dmitriy Parenskiy aka stream (dimparf@gmail.com)
 */
trait OrientDbSupport {

  //connect remote:localhost/emotioncity root varlogr3_
  //implicit val orientDb: ODatabaseDocumentTx =
    //new ODatabaseDocumentTx("remote:localhost/emotioncity").open("root", "varlogr3_")
  val oDatabaseDocumentPool = new OPartitionedDatabasePool("remote:localhost/emotiongraph", "root", "varlogr3_")
  val orientGraph = new OrientGraph(oDatabaseDocumentPool.acquire())
  implicit val orientDb: ODatabaseDocumentTx = orientGraph.getRawGraph

}



