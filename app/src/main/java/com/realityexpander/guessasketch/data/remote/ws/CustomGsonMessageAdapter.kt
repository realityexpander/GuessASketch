package com.realityexpander.guessasketch.data.remote.ws

import com.google.gson.Gson
import com.google.gson.JsonParser
import com.realityexpander.guessasketch.data.remote.ws.messageTypes.BaseMessageType
import com.realityexpander.guessasketch.data.remote.ws.messageTypes.SocketMessageType
import com.tinder.scarlet.Message
import com.tinder.scarlet.MessageAdapter
import java.lang.reflect.Type

class CustomGsonMessageAdapter<T> private constructor(
    private val gson: Gson
): MessageAdapter<T> {

    // Convert from JSON Message String to a BaseMessageType POJO
    override fun fromMessage(message: Message): T {
        val messageJson = when(message) {
            is Message.Text -> message.value
            is Message.Bytes -> message.value.toString()
        }

        // Create a json object from the message for easier handling
        val messageJsonObject = JsonParser.parseString(messageJson).asJsonObject

        // Extract the "type" of message
        val typeStr = messageJsonObject["type"].asString
            ?: throw IllegalArgumentException("Error: 'type' field not found in $messageJson")

        // Convert "type" to a socket message class to be used for gson deserialization
        val type = SocketMessageType.messageTypeMap[typeStr]
        ?: let {
            println("Error: Unknown socketType: $typeStr for $messageJson")
            BaseMessageType::class.java // throw IllegalArgumentException("Unknown message type")
        }

        try {
            // convert payload JSON string to the "type" of webSocket message
            val payload = gson.fromJson(messageJson, type)

            @Suppress("UNCHECKED_CAST")
            return payload!! as T
        } catch (e: Exception) {
            println("Error: Could not convert payload to $typeStr")
            throw e
        }
        //handleFrame(this, session.clientId, messageJson, payload)
    }

    // Convert from a BaseMessageType POJO to a JSON Message String
    override fun toMessage(data: T): Message {
        val dataJson = gson.toJson(data)
        return Message.Text(dataJson)
    }

    companion object {
        fun <T: BaseMessageType> create(type: Class<T>, gson: Gson): MessageAdapter<T> {
            return CustomGsonMessageAdapter(gson)
        }
    }

    class Factory(private val gson: Gson): MessageAdapter.Factory {
        override fun create(type: Type, annotations: Array<Annotation>): MessageAdapter<*> {
            return when(type) {
                BaseMessageType::class.java -> CustomGsonMessageAdapter<BaseMessageType>(gson)
                else -> throw IllegalArgumentException("Unsupported type: $type")
            }
        }

        // much more direct, but assumes only one type of websocket messages
//        override fun create(type: Type, annotations: Array<Annotation>): MessageAdapter<*> {
//            return CustomGsonMessageAdapter.create(BaseMessageType::class.java, gson)
//        }
    }

}