package com.realityexpander.guessasketch.data.remote.ws.messageTypes

import com.realityexpander.guessasketch.data.remote.ws.messageTypes.SocketMessageType.TYPE_JOIN_ROOM_HANDSHAKE

data class JoinRoomHandshake(
    val playerName: String,
    val roomName: String,
    val clientId: ClientId
): BaseMessageType(TYPE_JOIN_ROOM_HANDSHAKE)
