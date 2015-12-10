package com.emotioncity.soriento

import java.util.Date

import com.emotioncity.soriento.annotations.{Embedded, EmbeddedSet, Linked}
import org.scalatest.{BeforeAndAfter, FunSuite, Matchers}

/**
  * Created by stream on 13.05.15.
  */

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

class UseCaseTest extends FunSuite with Matchers with BeforeAndAfter with ODb with Dsl {

  /*test("Use case: create graph with linked and embedded documents in vertexes, navigate and extract case classes, save entity") {
    createOClass[Owner]
    val addressOClass = createOClass[Address]
    println(s"ADDRESS: $addressOClass")
    createOClass[Event]
    createOClass[Place]
    val owner = orientGraph.addVertex("class: Owner", "name", "OOO Studio Cafe")
    val address = Address(location = "Vladivostok").save
    val events = List(Event(name = "Free coffe day", new Date()), Event(name = "AM corp", new Date()))
    val place = orientGraph.addVertex("class: Place", "name", "Studio Cafe")
    place.setProperty("latitude", 23.5)
    place.setProperty("longitude", 568.9)
    place.setProperty("address", address)
    place.setProperty("events", events)
    place.addEdge("owner", owner)
    place.save
  }*/

  after {
    dropOClass[Owner]
    dropOClass[Address]
    dropOClass[Event]
    dropOClass[Place]
  }

}
