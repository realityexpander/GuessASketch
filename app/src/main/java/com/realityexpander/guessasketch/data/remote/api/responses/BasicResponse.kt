package com.realityexpander.guessasketch.data.remote.api.responses

data class BasicResponse(
    override val isSuccessful: Boolean,
    override val message: String? = null
) : BaseResponse


