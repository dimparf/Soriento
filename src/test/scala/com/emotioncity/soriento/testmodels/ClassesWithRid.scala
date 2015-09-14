package com.emotioncity.soriento.testmodels

import javax.persistence.Id

import com.orientechnologies.orient.core.id.ORID

/**
 * Created by stream on 11.08.15.
 */
case class ClassWithOptionalRid(@Id rid: Option[ORID] = None, name: String)
case class ClassWithRid(@Id rid: ORID = null, name: String)

case class ClassWithOptionalStringRid(@Id rid: Option[String] = None, name: String)
case class ClassWithStringRid(@Id rid: String, name: String)
