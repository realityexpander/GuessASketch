package com.realityexpander.guessasketch.data.remote.ws.messageTypes

import com.realityexpander.guessasketch.data.remote.ws.messageTypes.SocketMessageType.TYPE_ANNOUNCEMENT

data class Announcement(
    val message: String,
    val timestamp:  Long,
    val announcementType: Int
): BaseMessageType(TYPE_ANNOUNCEMENT) {

    companion object {
        const val TYPE_PLAYER_GUESSED_CORRECTLY = 0
        const val TYPE_PLAYER_JOINED = 1
        const val TYPE_PLAYER_EXITED_ROOM = 2
        const val TYPE_EVERYBODY_GUESSED_CORRECTLY = 3
    }
}
