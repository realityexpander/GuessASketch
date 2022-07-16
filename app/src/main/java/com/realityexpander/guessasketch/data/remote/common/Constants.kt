package com.realityexpander.guessasketch.data.remote.common

object Constants {

    private const val USE_LOCALHOST = true

    ////////////////////////////////////////////////
    // Rest SketchServer API - ends with a slash //
    ////////////////////////////////////////////////

    //private const val HTTP_BASE_URL_REMOTE = "https://82.180.173.232:8005/" // switch out for server ip
    private const val HTTP_BASE_URL_REMOTE = "https://guess-a-sketch-server.herokuapp.com/"  // for heroku address
    private const val HTTP_BASE_URL_LOCALHOST = "http://192.168.0.186:8005/"  // for hardware device
    private const val HTTP_BASE_URL_EMULATOR = "http://10.0.2.2:8005/" // for emulator

    val HTTP_BASE_URL = if (USE_LOCALHOST)
        HTTP_BASE_URL_LOCALHOST
    else
        HTTP_BASE_URL_REMOTE

    const val QUERY_PARAMETER_CLIENT_ID = "clientId"


    ///////////////////////////////////////////////////
    // Websockets Drawing API - ends WITHOUT a slash //
    ///////////////////////////////////////////////////

    // private const val WS_BASE_URL_REMOTE = "ws://82.180.173.232:8005/ws/draw"   // for ip-based server
    private const val WS_BASE_URL_REMOTE = "https://guess-a-sketch-server.herokuapp.com/ws/draw"      // for heroku server
    private const val WS_BASE_URL_LOCALHOST = "ws://192.168.0.186:8005/ws/draw"
    private const val WS_BASE_URL_EMULATOR = "ws://10.0.2.2:8005/ws/draw" // for emulator

    val WS_BASE_URL = if (USE_LOCALHOST)
        WS_BASE_URL_LOCALHOST
    else
        WS_BASE_URL_REMOTE

    const val WEBSOCKET_RECONNECT_INTERVAL = 3000L

}