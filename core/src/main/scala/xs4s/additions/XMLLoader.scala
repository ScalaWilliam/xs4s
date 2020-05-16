package xs4s.additions

import java.io._
import java.net.URL

import javax.xml.stream.XMLInputFactory
import xs4s.readElementFully

import scala.xml.Elem

/**
 * Utilities to replicate [[scala.xml.XML]]
 */
trait XMLLoader {
  def xmlInputFactory: XMLInputFactory
  def loadFile(file: File): Elem =
    readElementFully(xmlInputFactory.createXMLEventReader(new FileReader(file)))
  def loadFile(fd: FileDescriptor): Elem =
    readElementFully(xmlInputFactory.createXMLEventReader(new FileReader(fd)))
  def loadFile(name: String): Elem =
    readElementFully(xmlInputFactory.createXMLEventReader(new FileReader(name)))
  def load(is: InputStream): Elem =
    readElementFully(xmlInputFactory.createXMLEventReader(is))
  def load(reader: Reader): Elem =
    readElementFully(xmlInputFactory.createXMLEventReader(reader))
  def load(url: URL): Elem             = load(url.openStream())
  def loadString(string: String): Elem = load(new StringReader(string))
}
