import javax.persistence.Id

import com.emotioncity.soriento.annotations._
import com.orientechnologies.orient.core.id.ORID
import com.emotioncity.soriento.annotations.Embedded
import com.emotioncity.soriento.annotations.{LinkList, Linked}


package polymorphicmodels {


  /**
    * For testing loading polymorphic list Seq[TraceElement]
    *
    * TODO:  createOClass[<trait>] doesn't add trait's fields to the schema on the DB (although it does
    * create the class).
    */
  trait TraceElement {
    val date: Int
  }

  trait TraceElementWithID extends TraceElement {
    val id: Option[ORID]
  }

  case class LoginEvent(val userID: Int)

  case class TraceElementLoginEvent(@Id override val id: Option[ORID] = None,
                                    val date: Int,
                                    @Embedded val data: LoginEvent) extends TraceElementWithID

  case class ViewEvent(val userID: Int, val itemID: Int)

  case class TraceElementViewEvent(@Id override val id: Option[ORID] = None,
                                   val date: Int,
                                   @Embedded val data: ViewEvent) extends TraceElementWithID

  case class UserTrace(@Id val userID: Option[ORID] = None,
                       // Linked items will get an ID when unserialized.
                       // NB. This list is polymorphic
                       @LinkList val traceElements: List[TraceElementWithID]
                      )

}

package failingmodels {

  /**
    * For testing loading polymorphic list Seq[TraceElement] of generics.
    * Fails because oCreateClass saves "GenericTrace" as DB Type. Needs to be
    * "GenericTrace[LoginEvent]" or "GenericTrace_LoginEvent"
    * in order to name a concrete type that can be deserialized?
    *
    * NB. Does runtime type erasure may make this a non-issue (should use "GenericTrace"
    * as the type name and new a GenericTrace[Any] and load-time?)
    */
  trait TraceElement3 {
    val date: Int
  }

  case class GenericTrace[T](val date: Int, val data: T) extends TraceElement3

  /**
    * For polymorphic deserialization in class hierarchies.
    * Currently date is not deserialized if TraceElement2LoginEvent is saved.
    */
  class TraceElement2(val date: Long)

  case class TraceElement2LoginEvent(override val date: Long, val userID: Int) extends TraceElement2(date)


  case class TraceElement2ViewEvent(override val date: Long, val userID: Int, val itemID: Int) extends TraceElement2(date)

}

package basicmodels {


  case class Message(content: String)

  case class Blog(author: String, @Linked message: Message)

  // or @Linked
  case class BlogWithEmbeddedMessages(author: String, @LinkList messages: List[Message])

}

package reflectmodels {


  class TraceElement3(val id: Int, val date: Long)

  case class LoginEvent3(val userID: Int)

  case class TraceElementLoginEvent3(@Id override val id: Int,
                                     override val date: Long,
                                     @Embedded val data: LoginEvent3) extends TraceElement3(id, date)

}
