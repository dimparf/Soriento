package com.emotioncity.soriento.testmodels

import javax.persistence.Id

import WeekdayEnum._
import com.orientechnologies.orient.core.id.ORID

/**
  * Created by sir on 5/10/16.
  */
case class AllTypeFields(
                          val i: Int = 1,
                          val s: Short = 2,
                          val l: Long = 2,
                          val f: Float = 1.0f,
                          val d: Double = 1.0,
                          val b: Byte = 1,
                          val e: WeekdayEnum = MON, // scala.Enumeration
                          val iOpt: Option[Int] = Some(123),
                          //FAILING val iOptOpt: Option[Option[Int]] = Some(Some(123)),
                          val eOpt: Option[WeekdayEnum] = Some(MON),
                          //FAILING val eOptOpt: Option[Option[WeekdayEnum]] = Some(Some(MON)),
                          @Id id: Option[ORID] = None
                        ){
  def withNullIDs() = this.copy(id=None)
}
