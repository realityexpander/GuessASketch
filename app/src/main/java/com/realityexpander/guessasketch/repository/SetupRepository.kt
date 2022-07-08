package com.realityexpander.guessasketch.repository

import com.realityexpander.guessasketch.data.remote.common.Room
import com.realityexpander.guessasketch.data.remote.api.responses.BasicResponse
import com.realityexpander.guessasketch.util.Resource

interface SetupRepository {

    suspend fun createRoom(room: Room): Resource<BasicResponse>
//    suspend fun createRoom(room: Room): Resource<Unit> // We can use Unit type because we can just look at the Resource for success or failure

    suspend fun getRooms(searchQuery: String): Resource<List<Room>>

    suspend fun joinRoom(playerName: String, roomName: String): Resource<BasicResponse>
}