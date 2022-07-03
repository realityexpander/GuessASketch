package com.realityexpander.guessasketch.data.remote.responses

data class BasicApiResponse(
    override val isSuccessful: Boolean,
    override val message: String? = null
) : BaseResponse


