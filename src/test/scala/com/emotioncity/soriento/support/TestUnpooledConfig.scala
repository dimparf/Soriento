package com.emotioncity.soriento.support

import com.emotioncity.soriento.config.SorientoConfig
import com.orientechnologies.orient.core.db.OPartitionedDatabasePool

/**
  * Created by stream on 03.12.15.
  */
object TestUnpooledConfig extends SorientoConfig {
  override lazy val oDatabaseDocumentPool = None
}
