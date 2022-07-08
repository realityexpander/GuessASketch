package com.realityexpander.guessasketch.ui.drawing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.realityexpander.guessasketch.R
import com.realityexpander.guessasketch.data.remote.ws.DrawingApi
import com.realityexpander.guessasketch.data.remote.ws.messageTypes.*
import com.realityexpander.guessasketch.util.DispatcherProvider
import com.tinder.scarlet.WebSocket
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class DrawingViewModel @Inject constructor(
    private val drawingApi: DrawingApi,
    private val dispatcher: DispatcherProvider,
    private val gson: Gson
): ViewModel() {

    lateinit var playerName: String

    //////////////////////////////
    // UI State Events

    // Current selected "pick color" radio button
    private val _selectedColorButtonId = MutableStateFlow(R.id.rbBlack)
    val selectedColorButtonId: StateFlow<Int> = _selectedColorButtonId

    // Connection Progress bar visibility
    private val _connectionProgressBarVisible = MutableStateFlow(true)
    val connectionProgressBarVisible: StateFlow<Boolean> = _connectionProgressBarVisible

    // Choose Word Overlay visibility
    private val _chooseWordOverlayVisible = MutableStateFlow(false)
    val chooseWordOverlayVisible: StateFlow<Boolean> = _chooseWordOverlayVisible


    //////////////////////////////
    // WebSocket events

    // Can use this to wrap the websocket messagesTypes with more data (if necessary)
    data class MessageEvent<T: BaseMessageType>(val data: T, val type: String) //: SocketMessageEvent()

    // Socket connection events
    private val _socketConnectionEventChannel = Channel<WebSocket.Event>()
    val socketConnectionEvent = _socketConnectionEventChannel.receiveAsFlow().flowOn(dispatcher.io)

    // Socket BaseMessage events
    private val _socketBaseMessageEventChannel = Channel<BaseMessageType>()
    val socketBaseMessageEvent = _socketBaseMessageEventChannel.receiveAsFlow().flowOn(dispatcher.io)

    //////////////////////////////

    init {
        observeSocketConnectionEvents()
        observeSocketBaseMessages()
    }

    fun setChooseWordOverlayVisible(visible: Boolean) {
        _chooseWordOverlayVisible.value = visible
    }

    fun setConnectionProgressBarVisible(visible: Boolean) {
        _connectionProgressBarVisible.value = visible
    }

    fun selectColorRadioButton(id: Int) {
        _selectedColorButtonId.value = id
    }

    private fun observeSocketConnectionEvents() {  // observeEvents - todo remove at end
        viewModelScope.launch(dispatcher.io) {
            drawingApi.observeSocketConnectionEvents().collect { event ->
                when(event) {
                    is WebSocket.Event.OnConnectionOpened<*>,
                    is WebSocket.Event.OnConnectionClosed,
                    is WebSocket.Event.OnConnectionFailed -> {
                        _socketConnectionEventChannel.send(event)
                    }
                    else -> {
                        // do nothing since we will get all events (that we didnt handle) from the websocket
                        // including SocketBaseMessages

                        // println("SocketConnection unhandled Event: $event")
                        // Timber.d("observeSocketConnectionEvents - Unhandled socket event: $event")
                    }
                }
            }
        }
    }

    // Observe the websocket messages of BaseMessageType
    private fun observeSocketBaseMessages() {  // observeBaseModels - todo remove at end
        viewModelScope.launch(dispatcher.io) {
            drawingApi.observeBaseMessages().collect { message ->

                // println("observeSocketBaseMessages - message: $message")

                // Filter messages to be sent to the activity or handled here in the viewModel
                when(message) {
                    is DrawData,
                    is DrawAction,
                    is Announcement,
                    is GameError -> {
                        _socketBaseMessageEventChannel.send(message)
                    }
                    is Ping -> {
                        // respond with a pong (just another ping, really)
                        sendBaseMessageType(Ping(playerName))
                    }
                    else -> {
                        println("SocketMessage Event: $message")

                        // do nothing since we will get all events (that we didn't handle) from the websocket
                        // including SocketConnectionEvents

                        // _socketBaseMessageEventChannel.send(object: BaseMessageType(message.type){})
                        // Timber.e("DrawingViewModel - Unknown message type: ${
                        //     message.javaClass.simpleName
                        // }, ${
                        //     gson.toJson(message, message.javaClass)
                        // }")
                    }

                }

            }
        }
    }

    fun sendBaseMessageType(message: BaseMessageType) {
        viewModelScope.launch(dispatcher.io) {
            // println("Sending message: $message")
            drawingApi.sendBaseMessage(message)
        }
    }
}