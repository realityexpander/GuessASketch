package com.realityexpander.data.models.socket

import com.realityexpander.data.models.socket.SocketMessageType.TYPE_GAME_PHASE_UPDATE
import com.realityexpander.guessasketch.data.remote.common.Room

data class GamePhaseUpdate(
    var gamePhase: Room.GamePhase?,  // if not null, causes a phase change. If null, it's not serialized.
    var countdownTimerMillis: Long = 0L,
    val drawingPlayerName: String? = null,
): BaseMessageType(TYPE_GAME_PHASE_UPDATE)
