package com.realityexpander.guessasketch.data.remote.ws.messageTypes

import com.realityexpander.guessasketch.data.remote.ws.messageTypes.SocketMessageType.TYPE_PING

class Ping(val playerName: String? = null) : BaseMessageType(TYPE_PING)
