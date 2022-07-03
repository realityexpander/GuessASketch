package com.realityexpander.guessasketch.data.remote.common

data class Room(  // same as RoomResponse on server
    val roomName: String,
    val maxPlayers: Int,
    val playerCount: Int = 1
) {

    enum class GamePhase {
        INITIAL_STATE,       // no state yet.
        WAITING_FOR_PLAYERS,
        WAITING_FOR_START,
        NEW_ROUND,
        ROUND_IN_PROGRESS,  // game_running  // todo remove at end
        ROUND_ENDED,  // show_word // todo remove at end
    }

}
