package com.emotioncity.soriento

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx

/*
 * Copyright (c) 2014 Dmitriy Parenskiy aka stream (dimparf@gmail.com)
 */
trait OrientDbSupport {

  //connect remote:localhost/emotioncity root varlogr3_
  implicit val orientDb: ODatabaseDocumentTx =
    new ODatabaseDocumentTx("remote:localhost/emotioncity").open("root", "varlogr3_")
}



