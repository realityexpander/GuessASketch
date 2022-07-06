package com.realityexpander.data.models.socket

import com.realityexpander.data.models.socket.SocketMessageType.TYPE_NEW_WORDS

data class NewWords(
    val newWords: List<String>  // because this is a complex object, we cant send it as a json object, we need to wrap it
): BaseMessageType(TYPE_NEW_WORDS)
