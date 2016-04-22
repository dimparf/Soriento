package com.emotioncity.soriento.loadbyname

import com.emotioncity.soriento.ODocumentReader
import com.orientechnologies.orient.core.record.impl.ODocument


class ByClassNameODocumentReader(val readers: ClassNameReadersRegistry) extends ODocumentReader[Any] {
  def read(oDocument: ODocument): Any = {
    val name = oDocument.getClassName
    readers.readers.get(name) match {
      case Some(factory) => factory(oDocument)
      case None => throw new Exception(s"Unhandled type ${name}")
    }
  }
}



