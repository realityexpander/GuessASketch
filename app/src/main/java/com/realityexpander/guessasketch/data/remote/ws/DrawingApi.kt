package com.realityexpander.guessasketch.data.remote.ws

import com.realityexpander.data.models.socket.BaseMessageType
import com.tinder.scarlet.WebSocket
import com.tinder.scarlet.ws.Receive
import com.tinder.scarlet.ws.Send
import kotlinx.coroutines.flow.Flow

// Handles WebSocket messages
interface DrawingApi {

    @Receive
    fun observeSocketEvents(): Flow<WebSocket.Event>

    @Send
    fun sendBaseMessage(baseModel: BaseMessageType): Boolean  // true if successful // sendBaseModel todo remove at end

    @Receive
    fun observeBaseMessage(): Flow<BaseMessageType>  // observeBaseModel todo remove at end
}
