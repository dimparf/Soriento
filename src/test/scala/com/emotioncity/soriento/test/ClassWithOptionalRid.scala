package com.emotioncity.soriento.test

import javax.persistence.Id

import com.orientechnologies.orient.core.id.ORID

/**
 * Created by stream on 11.08.15.
 */
case class ClassWithOptionalRid(@Id rid: Option[ORID] = None, name: String)
