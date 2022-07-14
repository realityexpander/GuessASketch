package com.realityexpander.guessasketch.data.remote.ws.messageTypes

import com.realityexpander.guessasketch.data.remote.ws.messageTypes.SocketMessageType.TYPE_GAME_STATE

data class GameState(
    val drawingPlayerName: String? = null,
    val drawingPlayerClientId: ClientId? = null,
    val wordToGuess: String? = null
): BaseMessageType(TYPE_GAME_STATE)
