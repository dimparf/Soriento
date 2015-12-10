package com.emotioncity.soriento.testmodels

import com.emotioncity.soriento.ODocumentReader
import com.emotioncity.soriento.annotations.Embedded
import com.orientechnologies.orient.core.record.impl.ODocument
import com.emotioncity.soriento.RichODocumentImpl._



/**
 * Created by stream on 31.03.15.
 */
case class Home(name: String, @Embedded family: Family)
