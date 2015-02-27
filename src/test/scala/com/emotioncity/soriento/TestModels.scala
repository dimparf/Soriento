package com.emotioncity.soriento

/*
 *
 * Copyright (c) 2014 Dmitriy Parenskiy aka stream (dimparf@gmail.com)
*/

case class Test(field: String)
case class Message(text: String)
case class BlogWithLinkedMessages(name: String, @Linked message: Message)
case class BlogWithEmbeddedMessages(name: String, @Embedded message: Message)
case class BlogWithLinkSetMessages(name: String, @LinkSet messages: Set[Message])
case class BlogWithEmbeddedSetMessages(name: String, @EmbeddedSet messages: Set[Message])
case class Record(content: String)
case class Blog(author: String, @Embedded message: Record)
case class Simple(sField: String)
case class Complex(iField: Int, simple: Simple)
case class Checkin(location: String)
case class User(name: String, checkins: List[Checkin])
