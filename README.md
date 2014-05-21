xs4s
====

XML Streaming for Scala


This library shows how to use Scala to process XML streams.

Scala's scalability makes it easy to do XML stream processing with StAX. For example:

```scala

 val xmlEventReader = xmlInputFactory.createXMLEventReader(inputStream)

 case class InitialOpen(value: Double)
 case class Person(name: String, income: Double)

 // here we capture XML
 val captures = List(
   ("site" \ "open_auctions" \ "open_auction" \ "initial") {
     case initialElement =>
       Seq(InitialOpen(initialElement.text.toDouble))
   },
   ("site" \ "people" \ "person") {
     case personElement => for {
       name <- personElement \ "name" map (_.text)
       income = (personElement \ "profile" \ "@income").map(_.text.toDouble).headOption.getOrElse(0.0)
     } yield Person(name, income)
   }
 )

 val collectedData = TreeExtractor(captures).apply(xmlEventReader).toList

 <out>{
   for {
     Person(name, income) <- collectedData
     noItems = collectedData.count {
       case InitialOpen(value) if income > 5000 * value => true;
       case _ => false
     }
   } yield <items name={name}>{noItems.toString}</items>}</out>

```

This can consume 100MB files or 4GB files without any problems. And it does it fast. It converts XML streams into Scala XML trees on demand, which you can then query from.

The project contains the following files:

```
├── src
│   ├── main
│   │   └── scala
│   │       └── com
│   │           └── scalawilliam
|   │               ├── examples
|   │               │   ├── NumericNodesExample.scala
|   │               │   └── xml4s
|   │               │       ├── CarParksMapReduce.scala
|   │               │       ├── FindMostPopularKeywords2.scala
|   │               │       ├── FindMostPopularKeywords.scala
|   │               │       └── StreamedScalaQuery.scala
│   │               └── xml4s
│   │                   ├── ElementBuilder.scala
│   │                   ├── ScalaXmlStreamSplitter.scala
│   │                   ├── StreamedScalaQuery.scala
│   │                   └── XmlStreamSplitter.scala
```

ScalaWilliam <https://scalawilliam.com/>