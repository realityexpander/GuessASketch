package com.realityexpander.guessasketch.data.remote.responses

interface BaseResponse {
    val isSuccessful: Boolean
    val message: String?
}