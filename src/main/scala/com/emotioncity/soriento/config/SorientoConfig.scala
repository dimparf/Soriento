package com.emotioncity.soriento.config

import com.orientechnologies.orient.core.db.OPartitionedDatabasePool

/**
  * Created by stream on 03.12.15.
  */
trait SorientoConfig {
  def oDatabaseDocumentPool: Option[OPartitionedDatabasePool] = None
  def poolIsDefined: Boolean = oDatabaseDocumentPool.isDefined
  if (poolIsDefined) oDatabaseDocumentPool.get.acquire()
}

/*object TypesafeSorientoConfig extends SorientoConfig {
  override val oDatabaseDocumentPool = Option(new OPartitionedDatabasePool("remote:localhost/emotiongraph", "root", "varlogr3_").setAutoCreate(true))
}*/
