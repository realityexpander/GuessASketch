package com.realityexpander.guessasketch.data.remote.ws.messageTypes

data class PlayerData(
    val playerName: String,
    var isDrawing: Boolean = false,
    var score: Int = 0,
    var rank: Int = 0
)
