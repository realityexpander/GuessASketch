package com.realityexpander.guessasketch.data.remote.ws.messageTypes

import com.realityexpander.guessasketch.data.remote.ws.messageTypes.SocketMessageType.TYPE_ADD_ROOM

data class  AddRoom(
    val roomName: RoomName,
    val playerCapacity: Int
): BaseMessageType(TYPE_ADD_ROOM)
