package com.realityexpander.guessasketch.data.remote.api.responses

data class BasicResponseWithData<T>(
    override val isSuccessful: Boolean,
    override val message: String? = null,
    val data: T? = null
) : BaseResponse
