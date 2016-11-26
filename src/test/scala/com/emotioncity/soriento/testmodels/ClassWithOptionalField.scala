package com.emotioncity.soriento.testmodels

import javax.persistence.Id

import com.emotioncity.soriento.annotations.EmbeddedSet
import com.orientechnologies.orient.core.id.ORID

/**
 * Created by stream on 23.09.15.
 */
case class ClassWithOptionalField(simpleField: Int, optField: Option[String])
case class ClassWithOptionalLinkedField(@Id id: ORID, @EmbeddedSet simpleOptList: Option[List[Simple]])
