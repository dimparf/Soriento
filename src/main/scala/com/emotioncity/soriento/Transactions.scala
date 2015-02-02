package com.emotioncity.soriento

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx

/*
Copyright (c) 2014 Dmitriy Parenskiy aka stream (dimparf@gmail.com)
 */
trait Transactions {
  /**
   * Working in transactional fashion.
   * @param wrappedFunc function
   * @param db implicit db
   * @return T
   */
  def transaction[T](wrappedFunc: => T)(implicit db: ODatabaseDocumentTx): T = {
    try {
      db.begin()
      val result = wrappedFunc
      db.commit()
      result
    } catch {
      case e: Exception =>
        db.rollback
        throw e
    } finally {
          db.close()
    }
  }

}
