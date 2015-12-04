package com.emotioncity.soriento.support

import com.emotioncity.soriento.config.SorientoConfig
import com.orientechnologies.orient.core.db.OPartitionedDatabasePool

/**
  * Created by stream on 04.12.15.
  */
object TestPooledConfig extends SorientoConfig {
  override lazy val oDatabaseDocumentPool = Option(new OPartitionedDatabasePool("remote:localhost/emotiongraph", "root", "varlogr3_").setAutoCreate(true))
}
