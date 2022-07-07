package com.realityexpander.guessasketch.data.remote.ws.messageTypes

import com.realityexpander.guessasketch.data.remote.ws.messageTypes.SocketMessageType.TYPE_DRAW_ACTION


data class DrawAction(
    val action: String
): BaseMessageType(TYPE_DRAW_ACTION) {

    companion object {
        const val DRAW_ACTION_UNDO = "DRAW_ACTION_UNDO"
        const val DRAW_ACTION_DRAW = "DRAW_ACTION_DRAW"
        const val DRAW_ACTION_ERASE = "DRAW_ACTION_ERASE"
    }
}
