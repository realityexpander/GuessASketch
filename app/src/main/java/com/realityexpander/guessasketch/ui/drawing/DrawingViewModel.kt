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

    // Currently selected radio button
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

//    sealed class SocketMessageEvent {
        data class MessageEvent<T: BaseMessageType>(val data: T, val type: String) //: SocketMessageEvent()
        //object UndoEvent : SocketEvent()
//    }

    private val _socketConnectionEventChannel = Channel<WebSocket.Event>()
    val socketConnectionEvent = _socketConnectionEventChannel.receiveAsFlow().flowOn(dispatcher.io)

    // private val _socketMessageEventChannel = Channel<SocketMessageEvent>()
    // private val _socketMessageEventChannel = Channel<Event<*>>()
//    private val _socketMessageEventChannel = Channel<Event<BaseMessageType>>()
    private val _socketMessageEventChannel = Channel<BaseMessageType>()
    val socketMessageEvent = _socketMessageEventChannel.receiveAsFlow().flowOn(dispatcher.io)

    //////////////////////////////

    init {
        observeSocketEvents()
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

    private fun observeSocketEvents() {
        viewModelScope.launch(dispatcher.io) {
            drawingApi.observeSocketEvents().collect { event ->
                _socketConnectionEventChannel.send(event)
            }
        }
    }

    // Observe the websocket messages of BaseMessageType
    private fun observeSocketBaseMessages() {  // observeBaseModels - todo remove at end
        viewModelScope.launch(dispatcher.io) {
            drawingApi.observeBaseMessage().collect { message ->

                when(message) {
                    is DrawData,
                    is DrawAction,
                    is Announcement,
                    is GameError -> {
                        _socketMessageEventChannel.send(message)
                    }
                    is Ping -> {
                        sendMessage(Ping(playerName))
                    }
                    else -> {
                        _socketMessageEventChannel.send(object: BaseMessageType(message.type){})
                        Timber.e("DrawingViewModel - Unknown message type: ${message.javaClass.simpleName}")
                    }

                }

//                when (val messageType = message.type) {
//                    TYPE_DRAW_DATA,
//                    TYPE_DRAW_ACTION,
//                    TYPE_ANNOUNCEMENT,
//                    TYPE_GAME_ERROR -> {
//                        // _socketMessageEventChannel.send(SocketMessageEvent.Event(message, messageType))
//                        // _socketMessageEventChannel.send(Event(message, messageType))
//                        _socketMessageEventChannel.send(message)
//                    }
//                    TYPE_PING -> {
//                        sendMessage(Ping())
//                    }
//                    else -> {
//                        // _socketMessageEventChannel.send(SocketMessageEvent.Event(message, "UNEXPECTED_MESSAGE_TYPE"))
//                        // _socketMessageEventChannel.send(Event(message, "UNEXPECTED_MESSAGE_TYPE"))
//                        // _socketMessageEventChannel.send(object: BaseMessageType(type = "UNEXPECTED_MESSAGE_TYPE"){})
//                        _socketMessageEventChannel.send(object: BaseMessageType(messageType){})
//                    }
//                }
            }
        }
    }

    fun sendMessage(message: BaseMessageType) {
        viewModelScope.launch(dispatcher.io) {
            drawingApi.sendBaseMessage(message)
        }
    }
}