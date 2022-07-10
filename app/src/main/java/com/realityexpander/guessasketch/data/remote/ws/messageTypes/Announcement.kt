package com.realityexpander.guessasketch.data.remote.ws.messageTypes

import com.realityexpander.guessasketch.data.remote.ws.messageTypes.SocketMessageType.TYPE_ANNOUNCEMENT

data class Announcement(
    val message: String,
    val timestamp:  Long,
    val announcementType: Int
): BaseMessageType(TYPE_ANNOUNCEMENT) {

    companion object {
        const val ANNOUNCEMENT_PLAYER_GUESSED_CORRECTLY = 0
        const val ANNOUNCEMENT_PLAYER_JOINED_ROOM = 1
        const val ANNOUNCEMENT_PLAYER_EXITED_ROOM = 2
        const val ANNOUNCEMENT_EVERYBODY_GUESSED_CORRECTLY = 3
        const val ANNOUNCEMENT_NOBODY_GUESSED_CORRECTLY = 4
        const val ANNOUNCEMENT_GENERAL_MESSAGE = 5
    }
}
