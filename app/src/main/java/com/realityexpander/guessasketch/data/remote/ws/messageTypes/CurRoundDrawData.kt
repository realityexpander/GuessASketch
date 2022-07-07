package com.realityexpander.guessasketch.data.remote.ws.messageTypes

import com.realityexpander.guessasketch.data.remote.ws.messageTypes.SocketMessageType.TYPE_CUR_ROUND_DRAW_DATA

data class CurRoundDrawData(
    val data: List<String>
): BaseMessageType(TYPE_CUR_ROUND_DRAW_DATA)
