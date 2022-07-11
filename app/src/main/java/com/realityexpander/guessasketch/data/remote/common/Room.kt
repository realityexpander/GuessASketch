package com.realityexpander.guessasketch.data.remote.common

data class Room(  // same as RoomResponse on server
    val roomName: String,
    val maxPlayers: Int,
    val playerCount: Int = 1
) {

}
