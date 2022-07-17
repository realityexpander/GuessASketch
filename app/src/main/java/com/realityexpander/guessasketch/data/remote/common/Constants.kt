package com.realityexpander.guessasketch.data.remote.common

// To get local ip address of your dev machine:
//   in terminal:
//   ifconfig | grep inet | grep  broadcast | awk "{print \$2}"

object Constants {

    private const val USE_LOCALHOST = false
    private const val USE_LOCAL_EMULATOR = false
    private const val REMOTE_HOST_TYPE = "HEROKU"  // "HEROKU" or "UBUNTU"

    ////////////////////////////////////////////////////
    // REST/HTTP SketchServer API - ends with a slash //
    ////////////////////////////////////////////////////

    private const val HTTP_BASE_URL_REMOTE_UBUNTU = "http://82.180.173.232:8005/" // note: insecure http traffic, NOT https
    private const val HTTP_BASE_URL_REMOTE_HEROKU = "https://guess-a-sketch-server.herokuapp.com/"
    private const val HTTP_BASE_URL_LOCALHOST_PHYSICAL_HARDWARE = "http://192.168.0.186:8005/"  // ip address of your dev machine
    private const val HTTP_BASE_URL_LOCALHOST_EMULATOR = "http://10.0.2.2:8005/"

    val HTTP_BASE_URL = if (USE_LOCALHOST) {
        if(USE_LOCAL_EMULATOR) {
            HTTP_BASE_URL_LOCALHOST_EMULATOR
        } else {
            HTTP_BASE_URL_LOCALHOST_PHYSICAL_HARDWARE
        }
    } else {
        when(REMOTE_HOST_TYPE) {
            "HEROKU" -> HTTP_BASE_URL_REMOTE_HEROKU
            "UBUNTU" -> HTTP_BASE_URL_REMOTE_UBUNTU
            else -> throw IllegalArgumentException("Unknown http remote host type: $REMOTE_HOST_TYPE")
        }
    }

    const val QUERY_PARAMETER_CLIENT_ID = "clientId"

    ///////////////////////////////////////////////////
    // Websockets Drawing API - ends WITHOUT a slash //
    ///////////////////////////////////////////////////

    private const val WS_BASE_URL_REMOTE_UBUNTU = "ws://82.180.173.232:8005/ws/draw"   // note: insecure traffic, NO SSL
    private const val WS_BASE_URL_REMOTE_HEROKU = "https://guess-a-sketch-server.herokuapp.com/ws/draw"
    private const val WS_BASE_URL_LOCALHOST_PHYSICAL_HARDWARE = "ws://192.168.0.186:8005/ws/draw"  // ip address of your dev machine
    private const val WS_BASE_URL_LOCALHOST_EMULATOR = "ws://10.0.2.2:8005/ws/draw"

    val WS_BASE_URL = if (USE_LOCALHOST) {
        if(USE_LOCAL_EMULATOR) {
            WS_BASE_URL_LOCALHOST_EMULATOR
        } else {
            WS_BASE_URL_LOCALHOST_PHYSICAL_HARDWARE
        }
    } else {
        when(REMOTE_HOST_TYPE) {
            "HEROKU" -> WS_BASE_URL_REMOTE_HEROKU
            "UBUNTU" -> WS_BASE_URL_REMOTE_UBUNTU
            else -> throw IllegalArgumentException("Unknown websocket remote host type: $REMOTE_HOST_TYPE")
        }
    }

    const val WEBSOCKET_RECONNECT_INTERVAL = 3000L

}