package com.emotioncity.soriento.testmodels

import javax.persistence.Id

import WeekdayEnum._
import com.orientechnologies.orient.core.id.ORID

import scala.collection.JavaConverters._
import scala.collection.mutable.ArrayBuffer


case class Thing(x:Int)

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
                          val string:String = "beebop",
                          val e: WeekdayEnum = MON, // scala.Enumeration

                          // List of builtin types not working.
                          //                          val jListInt:java.util.List[Int] = AllTypeFields.javaIntList,
                          //                          //val jStringList:java.util.List[String] = List("hello").asJava,
                          //                          val iseq: Seq[Int] = Seq(1,2,3),
                          //                          val inseq: IndexedSeq[Int] = IndexedSeq(1,2,3),
                          //                          val iOpt: Option[Int] = Some(123),

                          val obj:Thing = new Thing(123),
                          val objISeq:IndexedSeq[Thing] = IndexedSeq(Thing(1), Thing(2)),
                          val objSeq:Seq[Thing] = Seq(Thing(1), Thing(2)),
                          val objMutISeq:collection.mutable.IndexedSeq[Thing] = ArrayBuffer(Thing(1), Thing(2)),
                          val objMutSeq:collection.mutable.Seq[Thing] = ArrayBuffer(Thing(1), Thing(2)),
                          val objImMutSeq:collection.immutable.Seq[Thing] = collection.immutable.Seq(Thing(1), Thing(2)),
                          val objImMutISeq:collection.immutable.Seq[Thing] = collection.immutable.IndexedSeq(Thing(1), Thing(2)),
                          val objArray:Array[Thing] = Array(Thing(1), Thing(2)),

                          //FAILING val iOptOpt: Option[Option[Int]] = Some(Some(123)),
                          val eOpt: Option[WeekdayEnum] = Some(MON),
                          //FAILING val eOptOpt: Option[Option[WeekdayEnum]] = Some(Some(MON)),
                          @Id id: Option[ORID] = None
                        ){
  def withNullIDs() = this.copy(id=None)
}

object AllTypeFields{

  val javaIntList = { val l = new java.util.LinkedList[Int](); l.add(99); l.add(101); l }
}