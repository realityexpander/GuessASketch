package com.realityexpander.guessasketch.data.remote.ws.messageTypes

import com.realityexpander.guessasketch.data.remote.ws.messageTypes.SocketMessageType.TYPE_ANNOUNCEMENT

data class Announcement(
    val message: String,
    val timestamp:  Long,
    val announcementType: String
): BaseMessageType(TYPE_ANNOUNCEMENT) {

    companion object {
        const val ANNOUNCEMENT_PLAYER_GUESSED_CORRECTLY = "ANNOUNCEMENT_PLAYER_GUESSED_CORRECTLY"
        const val ANNOUNCEMENT_PLAYER_JOINED_ROOM = "ANNOUNCEMENT_PLAYER_JOINED_ROOM"
        const val ANNOUNCEMENT_PLAYER_EXITED_ROOM = "ANNOUNCEMENT_PLAYER_EXITED_ROOM"
        const val ANNOUNCEMENT_EVERYBODY_GUESSED_CORRECTLY = "ANNOUNCEMENT_EVERYBODY_GUESSED_CORRECTLY"
        const val ANNOUNCEMENT_NOBODY_GUESSED_CORRECTLY = "ANNOUNCEMENT_NOBODY_GUESSED_CORRECTLY"
        const val ANNOUNCEMENT_GENERAL_MESSAGE = "ANNOUNCEMENT_GENERAL_MESSAGE"
    }
}
