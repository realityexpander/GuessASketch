package com.realityexpander.guessasketch.data.remote.ws.messageTypes

import com.realityexpander.guessasketch.data.remote.ws.messageTypes.SocketMessageType.TYPE_GAME_STATE

data class GameState(
    val drawingPlayerName: String,
    val drawingPlayerClientId: ClientId,
    val wordToGuess: String
): BaseMessageType(TYPE_GAME_STATE)
