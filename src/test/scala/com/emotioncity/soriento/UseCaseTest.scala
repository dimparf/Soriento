package com.emotioncity.soriento

import java.util.Date

import com.emotioncity.soriento.annotations.{EmbeddedSet, Embedded, Linked}
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentPool
import com.tinkerpop.blueprints.impls.orient.OrientGraph
import org.scalatest.{BeforeAndAfter, Matchers, FunSuite}

/**
 * Created by stream on 13.05.15.
 */
class UseCaseTest extends FunSuite with Matchers with BeforeAndAfter with ODb {

  val oDatabaseDocumentPool = new ODatabaseDocumentPool("plocal:/opt/oriendb/databases/emotioncity", "root", "varlogr3_")
  val orientGraph = new OrientGraph(oDatabaseDocumentPool.acquire())

  case class Owner(name: String)
  case class Address(location: String)
  case class Event(name: String, date: Date)
  case class Place(
    name: String,
    @Linked owner: Owner,
    latitude: Double,
    longitude: Double,
    @Embedded address: Address,
    @EmbeddedSet events: Set[Event] = Set.empty)

  test("Use case: create graph with linked and embedded documents in vertexes, navigate and extract case classes, save entity") {
    createOClass[Owner]
    createOClass[Address]
    createOClass[Event]
    createOClass[Place]

    

  }

  after {
    dropOClass[Owner]
    dropOClass[Address]
    dropOClass[Event]
    dropOClass[Place]
  }

  def initialize() = ???
}
