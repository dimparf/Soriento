package com.emotioncity.soriento.testmodels

import javax.persistence.Id

import com.orientechnologies.orient.core.id.ORID

/**
 * Created by stream on 24.09.15.
 */
case class ClassWithOptionalPrimitiveField(@Id id: ORID, doubleOpt: Option[Double])
