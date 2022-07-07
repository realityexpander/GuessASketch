package com.realityexpander.guessasketch.data.remote.ws.messageTypes

import com.realityexpander.guessasketch.data.remote.ws.messageTypes.SocketMessageType.TYPE_SET_WORD_TO_GUESS

data class SetWordToGuess(
    val wordToGuess: String,
    val roomName: String
): BaseMessageType(TYPE_SET_WORD_TO_GUESS)
