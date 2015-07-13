Soriento
========

## Scala to OrientDb object mapping library

Soriento is an object-relational mapping framework from scala case classes to OrientDb ODocument.

## Features

 - Creating/deleting OrientDb classes by case classes.
 - Transparent CRUD for documents represented as case classes.
 - Linked or Embedded definitions for case classes.
 - Support deserialization for case classes with @Embedded list/set of case classes.
 - Support OType mapping Scala => OrientDb OTypes.
 - Transactions support.
 - Query by SQL.
 
Supported types:
- Long, Double, Int, Float, Short, String, case class.

##Coming soon
- LinkList, LinkSet, LinkMap.
- EmbeddedList, EmbeddedSet, EmbeddedMap.

##Usage
Simple example:
```scala
  import com.emotioncity.Dsl._ // or extends Dsl trait

  case class Message(content: String)
  case class Blog(author: String, @Embedded message: Message) // or @Linked
  
  createOClass[Message]
  createOClass[Blog]
  
  val blog = Blog("Dim", message = Message("Hi")) //or without named params Blog("Dim", Message("Hi))
  blog.save
  
  val blogs: List[Blog] = db.queryBySql[Blog]("select from blog")
  
  deleteOClass[Message]
  deletOClass[Blog]
```

More examples in test directory.

## Testing
To run unit tests:

    sbt test

## Contributing

Welcome to contribute!
You can always post an issue or (even better) fork the project, implement your idea or fix the bug you have found and send a pull request. 
Just remember to test it when you are done. Here is how:

Run sbt test to run and compile tests.
    
## License
This software is available under the [New BSD License](LICENSE).    
