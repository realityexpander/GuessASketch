package com.realityexpander.guessasketch.data.remote.api

import com.realityexpander.guessasketch.data.remote.api.responses.BasicResponse
import com.realityexpander.guessasketch.data.remote.api.responses.BasicResponseWithData
import com.realityexpander.guessasketch.data.remote.common.Room
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

// To create, search and join a room on the server
interface SetupApi {

    @POST("/api/createRoom")
    suspend fun createRoom(
        @Body createRoomRequest: Room
    ): Response<BasicResponse>

    @GET("/api/getRooms")
    suspend fun getRooms(
        @Query("searchQuery") searchQuery: String
    ): Response<BasicResponseWithData<List<Room>>>

    @GET("/api/joinRoom")
    suspend fun joinRoom(
        @Query("roomName") roomName: String,
        @Query("playerName") playerName: String
    ): Response<BasicResponse>

}