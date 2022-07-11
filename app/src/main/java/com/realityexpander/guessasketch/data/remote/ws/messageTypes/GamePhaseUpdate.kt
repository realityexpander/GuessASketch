package com.realityexpander.guessasketch.data.remote.ws.messageTypes

import com.realityexpander.guessasketch.data.remote.ws.messageTypes.SocketMessageType.TYPE_GAME_PHASE_UPDATE

data class GamePhaseUpdate(
    var gamePhase: GamePhase? = null,  // if not null, causes a phase change. If null, it's not serialized.
    var countdownTimerMillis: Long = 0L,
    val drawingPlayerName: String? = null,
): BaseMessageType(TYPE_GAME_PHASE_UPDATE) {

    enum class GamePhase {
        INITIAL_STATE,       // no state yet.
        WAITING_FOR_PLAYERS,
        WAITING_FOR_START,
        NEW_ROUND,
        ROUND_IN_PROGRESS,
        ROUND_ENDED,
    }
}
