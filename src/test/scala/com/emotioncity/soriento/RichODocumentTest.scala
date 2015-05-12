package com.emotioncity.soriento

import java.util

import com.orientechnologies.orient.core.metadata.schema.OType
import com.orientechnologies.orient.core.record.impl.ODocument
import org.scalatest.{BeforeAndAfter, FunSuite, Matchers}

/**
 * Created by stream on 31.03.15.
 */

class RichODocumentTest extends FunSuite with Matchers with BeforeAndAfter with Dsl with OrientDbSupport with ODb {

  import Implicits._

  test("RichODocument should be provide implicit methods for read case class from ODocument") {
    val brothers: java.util.List[ODocument] = new util.ArrayList[ODocument]()
    brothers.add(new ODocument("Brother").field("name", "Blast").field("kulugda", "Morf"))
    brothers.add(new ODocument("Brother").field("name", "Faz").field("kulugda", "Morf2"))
    val oDocument = new ODocument("Blagda")
      .field("name", "Tost")
      .field("bio",
        new ODocument("Family")
          .field("mother", "Tata")
          .field("father", "Rembo")
          .field("brothers", brothers, OType.EMBEDDEDLIST
          ), OType.EMBEDDED)
    orientDb.save(oDocument)

    val blagdaList = orientDb.queryBySqlWithReader[Blagda]("select from Blagda")

    blagdaList.head should be equals Blagda("Tost", Family("Tata", "Rembo",
      List(Brother("Blast", Some("Morf")), Brother("Faz", None))))
  }

  after {
    dropOClass[Blagda]
    dropOClass[Brother]
    dropOClass[Family]
  }

  def initialize() = ???
}
