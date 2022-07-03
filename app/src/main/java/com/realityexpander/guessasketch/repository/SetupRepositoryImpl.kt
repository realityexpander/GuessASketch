package com.realityexpander.guessasketch.repository

import android.content.Context
import com.realityexpander.guessasketch.R
import com.realityexpander.guessasketch.data.remote.api.SetupApi
import com.realityexpander.guessasketch.data.remote.common.Room
import com.realityexpander.guessasketch.data.remote.responses.BasicApiResponse
import com.realityexpander.guessasketch.util.Resource
import com.realityexpander.guessasketch.util.checkForInternetConnection
import javax.inject.Inject

class SetupRepositoryImpl @Inject constructor(
    private val setupApi: SetupApi, // injected
    private val context: Context    // for connectivity check  (dont need for tests)
) : SetupRepository {

    override suspend fun createRoom(room: Room): Resource<BasicApiResponse> {
        if (!context.checkForInternetConnection())
            return Resource.Error(context.getString(R.string.error_internet_turned_off))

        return try {
            val response = setupApi.createRoom(room)

            if (response.isSuccessful) {
                if (response.body() != null) {
                    Resource.Success(response.body()?.message, response.body()!!)
                } else {
                    Resource.Error(
                        response.body()?.message
                            ?: context.getString(R.string.error_unknown)
                    )
                }
            } else {
                Resource.Error(response.message())
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: context.getString(R.string.error_unknown))
        }
    }

    override suspend fun getRooms(searchQuery: String): Resource<List<Room>> {
        if (!context.checkForInternetConnection())
            return Resource.Error(context.getString(R.string.error_internet_turned_off))

        return try {
            val response = setupApi.getRooms(searchQuery)

            return if (response.isSuccessful) {
                if (response.body() != null) {
                    val rooms = response.body()?.data

                    if (rooms != null) {
                        Resource.Success(response.body()?.message, rooms)
                    } else {
                        Resource.Error(
                            response.body()?.message
                                ?: context.getString(R.string.error_unknown)
                        )
                    }
                } else {
                    Resource.Error("No rooms found")
                }
            } else {
                Resource.Error(response.message())
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: context.getString(R.string.error_unknown))
        }
    }

    override suspend fun joinRoom(
        playerName: String,
        roomName: String
    ): Resource<BasicApiResponse> {
        if (!context.checkForInternetConnection())
            return Resource.Error(context.getString(R.string.error_internet_turned_off))

        return try {
            val response = setupApi.joinRoom(roomName, playerName)

            if (response.isSuccessful) {
                if (response.body() != null) {
                    Resource.Success(response.body()?.message, response.body()!!)
                } else {
                    Resource.Error(
                        response.body()?.message
                            ?: context.getString(R.string.error_unknown)
                    )
                }
            } else {
                Resource.Error(response.message())
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: context.getString(R.string.error_unknown))
        }
    }
}
