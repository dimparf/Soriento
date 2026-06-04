package com.emotioncity.soriento.testmodels

import javax.persistence.Id

import com.emotioncity.soriento.annotations.{LinkList, LinkSet, Linked}
import com.orientechnologies.orient.core.id.ORID

case class CCWithAllSupportedLinkedTypes(
  @Id id: Option[ORID] = None,
  @Linked simple: Simple,
  @LinkList list: List[Simple],
  @LinkSet set: Set[Simple]
)
