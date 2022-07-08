package com.realityexpander.guessasketch.data.remote.api.responses

interface BaseResponse {
    val isSuccessful: Boolean
    val message: String?
}