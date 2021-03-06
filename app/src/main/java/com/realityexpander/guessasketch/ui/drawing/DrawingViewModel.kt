package com.realityexpander.guessasketch.ui.drawing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.realityexpander.guessasketch.R
import com.realityexpander.guessasketch.data.remote.ws.DrawingApi
import com.realityexpander.guessasketch.data.remote.ws.messageTypes.*
import com.realityexpander.guessasketch.data.remote.ws.messageTypes.SocketMessageType.messageTypeMap
import com.realityexpander.guessasketch.ui.views.DrawingView
import com.realityexpander.guessasketch.util.CoroutineCountdownTimer
import com.realityexpander.guessasketch.util.DispatcherProvider
import com.tinder.scarlet.WebSocket
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.*
import javax.inject.Inject

// StateFlow current value is re-emitted after configuration change.
// Channels are "hot" and NOT re-emitted after configuration change.

@HiltViewModel
class DrawingViewModel @Inject constructor(
    private val drawingApi: DrawingApi,
    private val dispatcher: DispatcherProvider,
    private val gson: Gson
): ViewModel() {

    lateinit var playerName: String

    //////////////////////////////
    /// UI State Events        ///
    //////////////////////////////

    // Current selected "pick color" radio button id
    private val _selectedColorButtonId =
        MutableStateFlow(R.id.rbBlack)
    val selectedColorButtonId: StateFlow<Int> = _selectedColorButtonId

    // Connection Progress indicator visibility
    private val _isConnectionProgressBarVisible =
        MutableStateFlow(true)
    val isConnectionProgressBarVisible: StateFlow<Boolean> = _isConnectionProgressBarVisible

    // "Pick Word" Overlay visibility
    private val _isPickWordOverlayVisible =
        MutableStateFlow(false)
    val isPickWordOverlayVisible: StateFlow<Boolean> = _isPickWordOverlayVisible

    // Chat message List
    private val _chatMessages =
        MutableStateFlow<List<BaseMessageType>>(listOf())
    val chatMessages: StateFlow<List<BaseMessageType>> = _chatMessages

    // "Words To Pick" list (3 words that the drawing player picks one of to draw)
    private val _wordsToPick =
        MutableStateFlow(WordsToPick(listOf()))
    val wordsToPick: StateFlow<WordsToPick> = _wordsToPick

    // Game Phase Change
    private val _gamePhaseChange =
        MutableStateFlow(GamePhaseUpdate(GamePhaseUpdate.GamePhase.INITIAL_STATE))
    val gamePhaseChange: StateFlow<GamePhaseUpdate> = _gamePhaseChange

    // Game Phase Countdown Timer
    private val countdownTimer = CoroutineCountdownTimer()
    private var countdownTimerJob: Job? = null
    private val _gamePhaseTime =
        MutableStateFlow(0L)
    val gamePhaseTime: StateFlow<Long> = _gamePhaseTime

    // Game State
    private val _gameState =
        MutableStateFlow(GameState())
    val gameState: StateFlow<GameState> = _gameState

    // Drawing PathData Stack (for undo/redo)
    private val _pathDataStack =
        MutableStateFlow(Stack<DrawingView.PathData>())
    val pathDataStack: StateFlow<Stack<DrawingView.PathData>> = _pathDataStack

    // Players List (list of PlayerData)
    // Words To Pick (3 words that the drawing player picks one of to draw)
    private val _playersList =
        MutableStateFlow(PlayersList(listOf()))
    val playersList: StateFlow<PlayersList> = _playersList

    // Speech Recognition
    private val _isSpeechRecognizerListening =
        MutableStateFlow(false)
    val isSpeechRecognizerListening: StateFlow<Boolean> = _isSpeechRecognizerListening

    // Back button pressed for final exit from room
    private val _onFinalBackButtonPressed =
        MutableSharedFlow<Unit>(0)
    val onFinalBackButtonPressed: SharedFlow<Unit> = _onFinalBackButtonPressed

    //////////////////////////////
    /// WebSocket events       ///
    //////////////////////////////

    // Can use this to wrap the websocket messagesTypes with more data (if necessary)
    //   (not used in this project yet)
    data class MessageEvent<T: BaseMessageType>(val data: T, val type: String, val extra: String) //: SocketMessageEvent()

    // Socket connection events
    private val _socketConnectionEventChannel =
        Channel<WebSocket.Event>()
    val socketConnectionEventChannel =
        _socketConnectionEventChannel.receiveAsFlow().flowOn(dispatcher.io)

    // Socket BaseMessage events
    private val _socketBaseMessageEventChannel =
        Channel<BaseMessageType>()
    val socketBaseMessageEventChannel =
        _socketBaseMessageEventChannel.receiveAsFlow().flowOn(dispatcher.io)

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
        _isPickWordOverlayVisible.value = visible
    }

    fun setConnectionProgressBarVisible(visible: Boolean) {
        _isConnectionProgressBarVisible.value = visible
    }

    fun selectColorRadioButton(id: Int) {
        _selectedColorButtonId.value = id
    }

    fun setPathDataStack(stack: Stack<DrawingView.PathData>) {
        _pathDataStack.value = stack
    }

    private fun observeSocketConnectionEvents() {
        viewModelScope.launch(dispatcher.io) {
            drawingApi.observeSocketConnectionEvents().collect { event ->
                when(event) {
                    is WebSocket.Event.OnConnectionOpened<*>,
                    is WebSocket.Event.OnConnectionClosed,
                    is WebSocket.Event.OnConnectionFailed -> {
                        _socketConnectionEventChannel.send(event)
                    }
                    else -> {
                        // do nothing since we will get all events (that we didn't handle) from the websocket
                        // including SocketBaseMessages

                        Timber.w("observeSocketConnectionEvents - Unhandled socket event: $event")
                    }
                }
            }
        }
    }

    // Observe the websocket messages of BaseMessageType
    private fun observeSocketBaseMessages() {
        viewModelScope.launch(dispatcher.io) {
            drawingApi.observeBaseMessages().collect { message ->

                // println("observeSocketBaseMessages - message: $message")

                // Filter messages:
                //   1) to be sent to the activity,
                //   2) or handled here in the viewModel.
                //   3) A combination of 1 + 2.
                when(message) {
                    is DrawData,
                    is DrawAction,
                    is Announcement,
                    is ChatMessage,
                    is GameError -> {
                        _socketBaseMessageEventChannel.send(message)
                    }
                    is GameState -> {
                        _gameState.value = message
                    }
                    is PlayersList -> {
                        _playersList.value = message
                    }
                    is WordsToPick -> {
                        _wordsToPick.value = message
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

                        // If *NOT* WAITING_FOR_PLAYERS phase, set/sync timer to the GamePhaseUpdate timer
                        if (message.gamePhase != GamePhaseUpdate.GamePhase.WAITING_FOR_PLAYERS) {
                            // sync local timer to server timer
                            setGamePhaseCountdownTimer(message.countdownTimerMillis)
                        }
                    }
                    is CurRoundDrawData -> {
                        // Clear the current drawPathDataStack
                        //   (this data stream will re-create it, so that it takes into account any
                        //    new drawings or undo actions)
                        _socketBaseMessageEventChannel.send(DrawAction(DrawAction.DRAW_ACTION_ERASE))

                        // We get a message with a list of json strings that represents that draw data for the current round
                        // We need to convert this to a list of DrawData and DrawAction objects
                        message.data.forEach { json ->
                            // Convert json string to a jsonObject for easier parsing
                            val jsonObject = JsonParser.parseString(json).asJsonObject
                            val type = jsonObject.get("type").asString
                            type ?: throw IllegalStateException("CurRoundDrawData message type not found, type=$type")

                            // Convert the json string to a DrawData or DrawAction object
                            val drawMessage = gson.fromJson(json, messageTypeMap[type])
                            drawMessage ?: throw IllegalStateException("CurRoundDrawData drawMessage not found, type=$type")

                            _socketBaseMessageEventChannel.send(drawMessage)
                        }
                    }
                    else -> {
                        Timber.e("DrawingViewModel observeSocketConnectionEvents - Unhandled SocketMessage Event: $message")

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
    fun <T:BaseMessageType> sendBaseMessageType(data: T, callWhenDone: (() -> Unit)? = null) {
        viewModelScope.launch(dispatcher.io) {
            Timber.i("Sending message: ${data.javaClass.simpleName} --> $data")
            drawingApi.sendBaseMessage(data)

            // Call the callback when the message is sent
            callWhenDone?.invoke()
        }
    }

    // Send chat messages to the socket for the server to handle
    fun sendChatMessage(message: ChatMessage) {
        if (message.message.trim().isEmpty()) return

        viewModelScope.launch(dispatcher.io) {
            drawingApi.sendBaseMessage(message)
        }
    }

    fun sendTemporaryDisconnectRequest() {
        sendBaseMessageType(DisconnectTemporarilyRequest())
    }

    fun sendPermanentDisconnectRequest(callWhenDone: (() -> Unit)? = null) {
        sendBaseMessageType(DisconnectPermanentlyRequest(), callWhenDone)
    }

    fun finalBackButtonPressed() {
        viewModelScope.launch(dispatcher.main) {
            _onFinalBackButtonPressed.emit(Unit)
        }
    }

    ////////////////////////////////
    // Speech Recognizer          //
    ////////////////////////////////

    fun startSpeechRecognizerListening() {
        _isSpeechRecognizerListening.value = true
    }

    fun stopSpeechRecognizerListening() {
        _isSpeechRecognizerListening.value = false
    }

}