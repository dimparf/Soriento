package com.emotioncity.soriento.testmodels

import com.emotioncity.soriento.annotations.Linked


/**
 * Created by stream on 10.08.15.
 */
case class BlogWithLinkedMessage(name: String, @Linked message: Message)
