package com.realityexpander.guessasketch.data.remote.ws.messageTypes

import com.realityexpander.guessasketch.data.remote.ws.messageTypes.SocketMessageType.TYPE_CUR_ROUND_DRAW_DATA

data class CurRoundDrawData(
    val data: List<String>  // list of raw json strings of draw data that were sent to the server, represents the current round of drawing
): BaseMessageType(TYPE_CUR_ROUND_DRAW_DATA)
