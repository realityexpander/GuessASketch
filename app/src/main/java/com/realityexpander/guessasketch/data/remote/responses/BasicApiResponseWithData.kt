package com.realityexpander.guessasketch.data.remote.responses

data class BasicApiResponseWithData<T>(
    override val isSuccessful: Boolean,
    override val message: String? = null,
    val data: T? = null
) : BaseResponse
