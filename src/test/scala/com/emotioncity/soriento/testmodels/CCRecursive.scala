package com.emotioncity.soriento.testmodels

import javax.persistence.Id

import com.emotioncity.soriento.annotations.LinkList
import com.orientechnologies.orient.core.id.ORID

/**
  * Created by stream on 22.12.15.
  */
case class CCRecursive(@Id id: Option[ORID], sField: String, @LinkList rList: List[CCRecursive] = Nil)
