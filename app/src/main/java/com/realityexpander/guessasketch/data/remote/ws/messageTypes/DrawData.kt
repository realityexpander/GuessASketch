package com.realityexpander.guessasketch.data.remote.ws.messageTypes

import com.realityexpander.guessasketch.data.remote.ws.messageTypes.SocketMessageType.TYPE_DRAW_DATA

data class DrawData(
    val roomName: String,
    val color: Int,
    val strokeWidth: Float,
    val fromX: Float,
    val fromY: Float,
    val toX: Float,
    val toY: Float,
    val motionEvent: String, // DRAW_MOTION_EVENT_XXXXXX
): BaseMessageType(TYPE_DRAW_DATA) {

    companion object {
        const val DRAW_DATA_MOTION_EVENT_ACTION_DOWN = "DRAW_DATA_MOTION_EVENT_ACTION_DOWN"
        const val DRAW_DATA_MOTION_EVENT_ACTION_MOVE = "DRAW_DATA_MOTION_EVENT_ACTION_MOVE"
        const val DRAW_DATA_MOTION_EVENT_ACTION_UP = "DRAW_DATA_MOTION_EVENT_ACTION_UP"
    }
}
