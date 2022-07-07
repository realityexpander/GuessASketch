package com.realityexpander.guessasketch.data.remote.ws.messageTypes

import com.realityexpander.guessasketch.data.remote.ws.messageTypes.SocketMessageType.TYPE_CHAT_MESSAGE

data class ChatMessage(
    val fromClientId: ClientId,
    val fromPlayerName: String,
    val roomName: String,
    val message: String,
    val timestamp: Long
) : BaseMessageType(TYPE_CHAT_MESSAGE) {

//    override fun toMap(): Map<String, Any> {
//        return mapOf(
//            "from" to from,
//            "roomName" to roomName,
//            "message" to message,
//            "timestamp" to timestamp
//        )
//    }
}
