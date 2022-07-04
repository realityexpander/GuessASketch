package com.realityexpander.guessasketch.util

object Constants {

    private const val USE_LOCALHOST = true

    private const val HTTP_BASE_URL_REMOTE = "https://82.180.173.232/" // switch out for server ip
    private const val HTTP_BASE_URL_LOCALHOST = "http://192.168.0.186:8005/"  // for hardware device
    private const val HTTP_BASE_URL_EMULATOR = "http://10.0.2.2:8005/" // for emulator

    val HTTP_BASE_URL = if (USE_LOCALHOST)
            HTTP_BASE_URL_LOCALHOST
        else
            HTTP_BASE_URL_REMOTE

    const val MIN_PLAYER_NAME_LENGTH = 4
    const val MAX_PLAYER_NAME_LENGTH = 12

    const val MIN_ROOM_NAME_LENGTH = 4
    const val MAX_ROOM_NAME_LENGTH = 16

    const val SEARCH_TEXT_DEBOUNCE_DELAY_MILLIS = 300L
}
