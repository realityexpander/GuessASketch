package com.realityexpander.guessasketch.data.remote.ws.messageTypes

import com.realityexpander.guessasketch.data.remote.ws.messageTypes.SocketMessageType.TYPE_GAME_ERROR

data class GameError(
    val errorType: Int,
    val errorMessage: String? = null
): BaseMessageType(TYPE_GAME_ERROR) {

    companion object {
        const val ERROR_TYPE_ROOM_NOT_FOUND = 1
        val ERROR_TYPE_ROOM_NOT_FOUND_MSG = GameError(1,"Room not found")
    }
}