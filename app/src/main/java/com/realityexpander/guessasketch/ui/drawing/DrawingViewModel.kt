package com.realityexpander.guessasketch.ui.drawing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.realityexpander.guessasketch.R
import com.realityexpander.guessasketch.data.remote.common.Room
import com.realityexpander.guessasketch.data.remote.ws.DrawingApi
import com.realityexpander.guessasketch.data.remote.ws.messageTypes.*
import com.realityexpander.guessasketch.util.CoroutineCountdownTimer
import com.realityexpander.guessasketch.util.DispatcherProvider
import com.tinder.scarlet.WebSocket
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
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
    val pickWordOverlayVisible: StateFlow<Boolean> = _chooseWordOverlayVisible

    // Chat messages
    private val _chatMessages = MutableStateFlow<List<BaseMessageType>>(listOf())
    val chatMessages: StateFlow<List<BaseMessageType>> = _chatMessages

    // New words (pick 3 words)
    private val _wordsToPickHolder = MutableStateFlow(WordsToPickHolder(listOf()))
    val wordsToPickHolder: StateFlow<WordsToPickHolder> = _wordsToPickHolder

    // Game Phase Change
    private val _gamePhaseChange = MutableStateFlow(GamePhaseUpdate(Room.GamePhase.INITIAL_STATE))
    val gamePhaseChange: StateFlow<GamePhaseUpdate> = _gamePhaseChange

    // Game Phase Countdown Timer
    private val countdownTimer = CoroutineCountdownTimer()
    private var countdownTimerJob: Job? = null
    private val _gamePhaseTime = MutableStateFlow(0L)
    val gamePhaseTime: StateFlow<Long> = _gamePhaseTime

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

    private fun setGamePhaseCountdownTimer(durationMillis: Long) {
        countdownTimerJob?.cancel()
        countdownTimerJob = countdownTimer.timeAndEmitJob(durationMillis, viewModelScope) { timeLeftMillis ->
            _gamePhaseTime.value = timeLeftMillis
        }
    }

    fun cancelGamePhaseCountdownTimer() {
        countdownTimerJob?.cancel()
    }

    fun setPickWordOverlayVisible(visible: Boolean) {
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
                    is ChatMessage,
                    is SetWordToGuess,
                    is GameError -> {
                        _socketBaseMessageEventChannel.send(message)
                    }
                    is WordsToPickHolder -> {
                        _wordsToPickHolder.value = message
                        //_socketBaseMessageEventChannel.send(message)  // todo is this used at all? remove?
                    }
                    is Ping -> {
                        // respond with a pong (just another ping, really)
                        sendBaseMessageType(Ping(playerName))
                    }
                    is GamePhaseUpdate -> {

                        // Only change the game phase when the gamePhase is NOT null
                        // Note: null gamePhases are used to update the countdown timers
                        message.gamePhase?.let {
                            _gamePhaseChange.value = message
                        }

                        // Get the current countdown timer of the gamePhase (from the server every second)
                        _gamePhaseTime.value = message.countdownTimerMillis

                        // If *NOT* WAITING_FOR_PLAYERS phase, set/sync timer to the GamePhaseUpdate
                        if (message.gamePhase != Room.GamePhase.WAITING_FOR_PLAYERS) {
                            setGamePhaseCountdownTimer(message.countdownTimerMillis)
                        }
                    }
                    else -> {
                        println("DrawingViewModel observeSocketConnectionEvents - Unhandled SocketMessage Event: $message")

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

    // Send messages to the socket for the server to handle
    fun sendBaseMessageType(data: BaseMessageType) {
        viewModelScope.launch(dispatcher.io) {
            println("Sending message: $data")
            drawingApi.sendBaseMessage(data)
        }
    }

    // Send chat messages to the socket for the server to handle
    fun sendChatMessage(message: ChatMessage) {
        if (message.message.trim().isEmpty()) return

        viewModelScope.launch(dispatcher.io) {
            // println("Sending message: $message")
            drawingApi.sendBaseMessage(message)
        }
    }
}