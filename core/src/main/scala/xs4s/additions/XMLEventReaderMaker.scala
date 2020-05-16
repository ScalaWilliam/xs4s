package xs4s.additions

import java.io._
import java.net.URL

import javax.xml.stream.{XMLEventReader, XMLInputFactory}

/**
  * Utilities to create an XMLEventReader from Scala more easily
  */
trait XMLEventReaderMaker {
  def xmlInputFactory: XMLInputFactory
  def fromFile(file: File): XMLEventReader =
    xmlInputFactory.createXMLEventReader(new FileReader(file))
  def fromFileDescriptor(fd: FileDescriptor): XMLEventReader =
    xmlInputFactory.createXMLEventReader(new FileReader(fd))
  def fromFilename(name: String): XMLEventReader =
    xmlInputFactory.createXMLEventReader(new FileReader(name))
  def fromInputStream(is: InputStream): XMLEventReader =
    xmlInputFactory.createXMLEventReader(is)
  def fromReader(reader: Reader): XMLEventReader =
    xmlInputFactory.createXMLEventReader(reader)
  def fromURL(url: URL): XMLEventReader = fromInputStream(url.openStream())
  def fromString(string: String): XMLEventReader =
    fromReader(new StringReader(string))
}
