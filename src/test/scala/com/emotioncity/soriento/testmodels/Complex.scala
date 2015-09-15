package com.emotioncity.soriento.testmodels

import com.emotioncity.soriento.ODocumentReader
import com.emotioncity.soriento.RichODocumentImpl._
import com.emotioncity.soriento.annotations.{Embedded, EmbeddedList}
import com.orientechnologies.orient.core.record.impl.ODocument

/**
 * Created by stream on 07.07.15.
 */
case class Complex(iField: Int, @Embedded simple: Simple, sField: String, @EmbeddedList listField: List[Simple])

