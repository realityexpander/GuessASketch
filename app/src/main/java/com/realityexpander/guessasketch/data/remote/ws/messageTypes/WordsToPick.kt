package com.realityexpander.guessasketch.data.remote.ws.messageTypes

import com.realityexpander.guessasketch.data.remote.ws.messageTypes.SocketMessageType.TYPE_WORDS_TO_PICK

data class WordsToPick(
    val words: List<String>  // because this is a complex object, we cant send it as a json object, we need to wrap it
): BaseMessageType(TYPE_WORDS_TO_PICK)
