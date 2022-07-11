package com.realityexpander.guessasketch.data.remote.ws.messageTypes

import com.realityexpander.guessasketch.data.remote.ws.messageTypes.SocketMessageType.TYPE_PLAYERS_LIST

data class PlayersList(
    val players: List<PlayerData>
): BaseMessageType(TYPE_PLAYERS_LIST)
