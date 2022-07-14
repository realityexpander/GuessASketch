package com.realityexpander.guessasketch.ui.drawing

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.TypedValue
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.core.view.isVisible
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.*
import androidx.navigation.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.realityexpander.guessasketch.R
import com.realityexpander.guessasketch.data.remote.ws.messageTypes.*
import com.realityexpander.guessasketch.data.remote.ws.messageTypes.DrawAction.Companion.DRAW_ACTION_DRAW
import com.realityexpander.guessasketch.data.remote.ws.messageTypes.DrawAction.Companion.DRAW_ACTION_ERASE
import com.realityexpander.guessasketch.data.remote.ws.messageTypes.DrawAction.Companion.DRAW_ACTION_UNDO
import com.realityexpander.guessasketch.data.remote.ws.messageTypes.DrawData.Companion.DRAW_DATA_MOTION_EVENT_ACTION_DOWN
import com.realityexpander.guessasketch.data.remote.ws.messageTypes.DrawData.Companion.DRAW_DATA_MOTION_EVENT_ACTION_MOVE
import com.realityexpander.guessasketch.data.remote.ws.messageTypes.DrawData.Companion.DRAW_DATA_MOTION_EVENT_ACTION_UP
import com.realityexpander.guessasketch.databinding.ActivityDrawingBinding
import com.realityexpander.guessasketch.di.CLIENT_ID
import com.realityexpander.guessasketch.ui.adapters.ChatMessageAdapter
import com.realityexpander.guessasketch.ui.adapters.PlayerAdapter
import com.realityexpander.guessasketch.ui.common.Constants.SPEECH_RECOGNIZER_MAX_NUM_WORDS_MAX_RESULTS
import com.realityexpander.guessasketch.ui.dialogs.LeaveDialog
import com.realityexpander.guessasketch.ui.views.DrawingView
import com.realityexpander.guessasketch.util.Constants
import com.realityexpander.guessasketch.util.hideKeyboard
import com.tinder.scarlet.WebSocket
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import pub.devrel.easypermissions.AppSettingsDialog
import pub.devrel.easypermissions.EasyPermissions
import timber.log.Timber
import java.util.*
import javax.inject.Inject
import javax.inject.Named

typealias ColorInt = Int // like @ColorInt
typealias ResId = Int // like @ResId

const val CAN_SCROLL_DOWN = 1 // List scroll position is NOT at the bottom, ie: Can scroll down if needed
const val REQUEST_CODE_RECORD_AUDIO = 10001  // Request code for record audio

@AndroidEntryPoint
class DrawingActivity:
    AppCompatActivity(),
    LifecycleObserver,
    EasyPermissions.PermissionCallbacks, // for audio recording permissions for the speech recognizer
    RecognitionListener // for speech recognizer
{
    private lateinit var binding: ActivityDrawingBinding

    private val viewModel: DrawingViewModel by viewModels()
    private val args by navArgs<DrawingActivityArgs>()

    @Inject
    @Named(CLIENT_ID)
    lateinit var clientId: String

    private var isDrawingPlayer = false

    // Color radio buttons
    private lateinit var resourceColorToColorButtonIdMap: Map<Int, Int>
    private var curDrawingColor: Int = Color.BLACK

    // Chat message list
    private lateinit var rvChatMessageAdapter: ChatMessageAdapter

    // Drawer menu (List of players, rank, score)
    private lateinit var toggleDrawer: ActionBarDrawerToggle
    @Inject  // because the PlayerAdapter constructor as no args, we have injected it
    lateinit var rvPlayersAdapter: PlayerAdapter

    // Speech recognition
    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var speechRecognizerIntent: Intent


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDrawingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel.playerName = args.playerName

        // Setup the LifeCycleObserver
        // (for when activity finally completely stops, not just paused for permissions
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)

        // Setup the map of resource colors to radio button ids
        resourceColorToColorButtonIdMap = getColorToColorButtonIdMap()

        // Select the color of the drawing player's pen
        binding.colorGroup.setOnCheckedChangeListener { _, checkedId ->
            viewModel.selectColorRadioButton(checkedId)
        }

        // Undo (only available to drawing player)
        binding.ibUndo.setOnClickListener {
            if(binding.drawingView.isEnabled) {  // Only undo if drawing is enabled (this user is the drawing player)
                binding.drawingView.undo()
                viewModel.sendBaseMessageType(DrawAction(DRAW_ACTION_UNDO))
                selectColor(curDrawingColor)  // make sure the current color is selected
            }
        }

        // Clear text message
        binding.ibClearText.setOnClickListener {
            binding.etMessage.text?.clear()
        }

        // Send chat message
        binding.ibSend.setOnClickListener {
            viewModel.sendChatMessage(
                ChatMessage(
                    fromClientId = clientId,
                    fromPlayerName = args.playerName,
                    message = binding.etMessage.text.toString(),
                    timestamp = System.currentTimeMillis(),
                    roomName = args.roomName
                )
            )
            binding.etMessage.text?.clear()

            // reset the text view to default size
            binding.etMessage.height = binding.etMessage.lineHeight

            hideKeyboard(binding.root)
        }

        // Drawing Path Data Stack listener
        binding.drawingView.setPathDataStackChangedListener { pathStack ->
            viewModel.setPathStackData(pathStack)
        }

        // Setup Speech recognition
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        speechRecognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            //putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.US)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, packageName)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, SPEECH_RECOGNIZER_MAX_NUM_WORDS_MAX_RESULTS)
        }
        if(SpeechRecognizer.isRecognitionAvailable(this)) {
            speechRecognizer.setRecognitionListener(this)
        } else {
            Timber.w("Speech recognition is not available")
        }

        // Setup speech recognition button
        binding.ibMic.setOnClickListener {

            // if was NOT listening and we have permission to record audio, start listening
            if(!viewModel.isSpeechRecognizerListening.value && isRecordAudioPermissionInManifest()) {
                viewModel.startSpeechRecognizerListening()
                return@setOnClickListener
            }

            // if want to start listening, but we don't have permission to record audio, ask for permission
            if (!viewModel.isSpeechRecognizerListening.value) {
                requestRecordAudioPermission()
                return@setOnClickListener
            }

            // if was listening, switch it off now.
            viewModel.stopSpeechRecognizerListening()
        }


        /////////////////////////
        // setup UI components

        setupNavDrawer()
        setupChatMessageRecyclerView()

        /////////////////////////
        // Setup event listeners

        listenToUiStateEvents()

        listenToSocketConnectionEvents()
        listenToSocketBaseMessageEvents()

        setupDrawingViewTouchListenerToSendDrawDataToServer(binding.drawingView)
    }

    ///////////////////////////////////
    // Setup UI                      //
    ///////////////////////////////////

    // Setup the drawer for the recyclerview list of players
    private fun setupNavDrawer() {
        toggleDrawer = ActionBarDrawerToggle(this, binding.root, R.string.open, R.string.close)
        toggleDrawer.syncState()

        val navHeader = layoutInflater.inflate(R.layout.nav_drawer_header, binding.navView)
        binding.root.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED) // only can be opened by clicking the "players" button
        binding.ibPlayersDrawerOpen.setOnClickListener {
            binding.root.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
            binding.root.openDrawer(GravityCompat.START)
        }

        binding.root.addDrawerListener( object : DrawerLayout.DrawerListener {
            override fun onDrawerSlide(drawerView: View, slideOffset: Float) = Unit
            override fun onDrawerOpened(drawerView: View) = Unit
            override fun onDrawerStateChanged(newState: Int) = Unit

            // Lock the drawer so it cant be opened accidentally when drawing
            override fun onDrawerClosed(drawerView: View) {
                binding.root.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
            }

        })

        setupPlayerListInNavDrawer(navHeader)
    }

    // For ActionBarDrawer
    //   This is used if there is a menu item in the action bar that opens the drawer
    //   Currently, this is not used, but it is here if we want to add it in the future
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if(toggleDrawer.onOptionsItemSelected(item)) {
            return true
        }
        return super.onOptionsItemSelected(item)
    }


    ///////////////////////////////////
    // Listen to ViewModel events    //
    ///////////////////////////////////

    // Listen to UI state updates from the view model
    private fun listenToUiStateEvents() {
        // We have separate lifecycle scope for each UI State event, because when
        // the coroutine is suspended, it wouldn't process events for any UI items below it.
        // ie: If we put all UI State Events in the selectedColorButtonId scope, only when the user
        //   taps on the color radio button would the other UI elements be processed.

        // Color of the paintbrush
        lifecycleScope.launchWhenStarted {
            viewModel.selectedColorButtonId.collect { buttonId ->
                binding.colorGroup.check(buttonId)  // select the correct radio button

                // Set the paint color of the drawing view
                //   ** this is a reverse search for color (key) by searching for the button id (value)
                curDrawingColor = resourceColorToColorButtonIdMap.entries.firstOrNull { colorToButtonId ->
                    colorToButtonId.value == buttonId
                }?.key ?: Color.BLACK
                selectColor(curDrawingColor)

            }
        }

        // Connection Progress Bar
        lifecycleScope.launchWhenStarted {
            viewModel.isConnectionProgressBarVisible.collect { isVisible ->
                binding.connectionProgressBar.visibility = if (isVisible) View.VISIBLE else View.GONE
            }
        }

        // Pick Word Overlay visibility
        lifecycleScope.launchWhenStarted {
            viewModel.isPickWordOverlayVisible.collect { isVisible ->
                binding.pickWordOverlay.visibility = if (isVisible) View.VISIBLE else View.GONE
            }
        }

        // Chat messages
        lifecycleScope.launchWhenStarted {
            viewModel.chatMessages.collect { chatItems ->
                if(rvChatMessageAdapter.chatItems.isEmpty()) {
                    updateChatMessages(chatItems)
                }
            }
        }

        // WordsToPickHolder -> SetWordToGuess
        lifecycleScope.launchWhenStarted {
            viewModel.wordsToPick.collect { wordsToPick ->
                val words = wordsToPick.words
                if(words.isEmpty()) return@collect

                // Let drawing player choose a word for other players to guess based on his drawing
                binding.apply {
                    btnFirstWord.text = words[0]
                    btnSecondWord.text = words[1]
                    btnThirdWord.text = words[2]
                    viewModel.setPickWordOverlayVisible(true)

                    btnFirstWord.setOnClickListener {
                        sendSetWordToGuessMessage(words[0], args.roomName)
                        viewModel.setPickWordOverlayVisible(false)
                    }

                    btnSecondWord.setOnClickListener {
                        sendSetWordToGuessMessage(words[1], args.roomName)
                        viewModel.setPickWordOverlayVisible(false)
                    }

                    btnThirdWord.setOnClickListener {
                        sendSetWordToGuessMessage(words[2], args.roomName)
                        viewModel.setPickWordOverlayVisible(false)
                    }
                }
            }
        }

        // Game Phase Time Update - update the round timer & Progress bar
        lifecycleScope.launchWhenStarted {
            viewModel.gamePhaseTime.collect { time ->
                binding.roundTimerProgressBar.progress = time.toInt()  // uses `.max` value as maximum

                binding.tvPickWordTimeRemaining.text = (time/1000L).toString() // if its visible
            }
        }

        // Game State Update
        lifecycleScope.launchWhenStarted {
            viewModel.gameState.collect { gameState ->
                binding.apply {

                    // Initial state?
                    gameState.drawingPlayerName?: return@collect  // if so, ignore it (it's the initial state)
                    gameState.drawingPlayerClientId?: return@collect  // if so, ignore it (it's the initial state)
                    gameState.wordToGuess?: return@collect  // if so, ignore it (it's the initial state)

                    // Gives the "word to guess" actual word to the drawing player.
                    // Server will send the "underscored" word to non-drawing players.
                    tvWordToGuessOrStatusMessage.text = gameState.wordToGuess

                    // Set these again (in case of config change)
                    //isDrawingPlayer = gameState.drawingPlayerName == args.playerName // old way, left for reference
                    isDrawingPlayer = gameState.drawingPlayerClientId == clientId
                    setDrawingIsEnabledAndColorButtonsVisible(isDrawingPlayer) // only the drawingPlayer can change the drawing color

                    // NOTE: drawing player can still use the chat Messages
                    // setChatMessageInputIsVisible(!isDrawingPlayer)  // this would disable the chat input for drawing playerp
                }
            }
        }

        // Game Phase Change (Time only updates are handled in gamePhaseTime)
        lifecycleScope.launchWhenStarted {
            viewModel.gamePhaseChange.collect { gamePhaseChange ->

                println("gamePhaseChange = $gamePhaseChange")

                when(gamePhaseChange.gamePhase) {
                    GamePhaseUpdate.GamePhase.INITIAL_STATE -> {
                        // do nothing

                        setDrawingIsEnabledAndColorButtonsVisible(false)
                        setChatMessageInputIsVisible(false)
                    }
                    GamePhaseUpdate.GamePhase.WAITING_FOR_PLAYERS -> {
                        binding.apply {
                            roundTimerProgressBar.isIndeterminate = true
                            tvWordToGuessOrStatusMessage.text = getString(R.string.waiting_for_players)
                        }
                        viewModel.cancelGamePhaseCountdownTimer()
                        viewModel.setConnectionProgressBarVisible(false)
                        setChatMessageInputIsVisible(true)

                        // hide the pick word overlay in case players have left
                        //   and we are back in the waiting phase.
                        viewModel.setPickWordOverlayVisible(false)
                    }
                    GamePhaseUpdate.GamePhase.WAITING_FOR_START -> {
                        binding.apply {
                            roundTimerProgressBar.max = gamePhaseChange.countdownTimerMillis.toInt()
                            roundTimerProgressBar.progress = roundTimerProgressBar.max
                            roundTimerProgressBar.isIndeterminate = false
                            tvWordToGuessOrStatusMessage.text = getString(R.string.waiting_for_start)
                            setChatMessageInputIsVisible(true)
                        }
                    }
                    GamePhaseUpdate.GamePhase.NEW_ROUND -> {
                        binding.apply {
                            roundTimerProgressBar.max = gamePhaseChange.countdownTimerMillis.toInt() // set the max value of the progress bar to the round time
                            gamePhaseChange.drawingPlayerName?.let { drawingPlayerName ->
                                tvWordToGuessOrStatusMessage.text = getString(R.string.drawing_player_is_choosing_word, drawingPlayerName)
                            }

                            drawingView.isEnabled = false // no one can draw while the word is being chosen
                            drawingView.clearDrawing() // clear the drawing view
                            selectColor(Color.BLACK) // reset drawing color to black

                            // Is this the drawing player? If yes, show the pick word overlay
                            isDrawingPlayer = gamePhaseChange.drawingPlayerName == args.playerName
                            viewModel.setPickWordOverlayVisible(isDrawingPlayer) // only the drawing player can choose a word

                            // disable drawing for everyone, except when player is drawing player
                            setDrawingIsEnabledAndColorButtonsVisible(isDrawingPlayer)

                            setChatMessageInputIsVisible(true) // everyone can use the chat
                        }
                    }
                    GamePhaseUpdate.GamePhase.ROUND_IN_PROGRESS -> {
                        binding.apply {
                            roundTimerProgressBar.max = gamePhaseChange.countdownTimerMillis.toInt() // set the max value of the progress bar to the round time
                            viewModel.setPickWordOverlayVisible(false) // no one can pick the word anymore
                            setChatMessageInputIsVisible(true)

                            if (gamePhaseChange.drawingPlayerName == args.playerName) {
                                setDrawingIsEnabledAndColorButtonsVisible(true)
                            }
                        }
                    }
                    GamePhaseUpdate.GamePhase.ROUND_ENDED -> {
                        binding.apply {
                            roundTimerProgressBar.max = gamePhaseChange.countdownTimerMillis.toInt() // set the max value of the progress bar to the round time
                            drawingView.isEnabled = false // no one can draw while the word is being shown
                            setDrawingIsEnabledAndColorButtonsVisible(false) // no one can draw
                            setChatMessageInputIsVisible(true) // everyone can still chat

                            // Finish the drawing if the player is currently drawing. (Force the stopTouch)
                            if (drawingView.isCanvasDrawing) {

                                drawingView.apply {

                                    // Send the closing ACTION_UP message to the server
                                    viewModel.sendBaseMessageType(
                                        createDrawDataForServer(
                                            getCurrentX(),
                                            getCurrentY(),
                                            getCurrentX(),
                                            getCurrentY(),
                                            DRAW_DATA_MOTION_EVENT_ACTION_UP
                                        )
                                    )

                                    // Finish the drawing locally
                                    stopTouchExternally()
                                }
                            }
                        }
                    }
                    else -> {
                        Timber.DebugTree().e("DrawingActivity - Unexpected GamePhaseUpdate type: ${gamePhaseChange.gamePhase}")
                    }
                }
            }
        }

        // Players List Update
        lifecycleScope.launchWhenStarted {
            viewModel.playersList.collect { playersList ->
                binding.apply {
                    updatePlayersList(playersList.players)
                }
            }
        }

        // Speech Recognition listening state update
        lifecycleScope.launchWhenStarted {
            viewModel.isSpeechRecognizerListening.collect { isListening ->
                if(isListening && !SpeechRecognizer.isRecognitionAvailable(this@DrawingActivity)) {
                    showToast(getString(R.string.speech_recognizer_not_available))
                    binding.ibMic.isEnabled = false
                } else {
                    binding.ibMic.isEnabled = true
                    setSpeechRecognizerIsListening(isListening)
                }
            }
        }

        // onFinalBackButtonPressed
        lifecycleScope.launchWhenStarted {
            viewModel.onFinalBackButtonPressed.collect {
                super.onBackPressed() // call the super method to exit the activity
            }
        }
    }

    private fun listenToSocketBaseMessageEvents() {
        lifecycleScope.launchWhenStarted {
            viewModel.socketBaseMessageEventChannel.collect { message ->

                when(message) {
                    is DrawData -> {
                        // only draw the server data if this user is NOT the drawing player
                        // todo needed? currently server only sends the drawData to the non-drawing players
                        //    unless its updating the drawing player when rejoining the room.
                        //if(binding.drawingView.isEnabled) return@collect

                        when (message.motionEvent) {
                            DRAW_DATA_MOTION_EVENT_ACTION_DOWN,
                            DRAW_DATA_MOTION_EVENT_ACTION_MOVE,
                            DRAW_DATA_MOTION_EVENT_ACTION_UP -> {
                                renderDrawDataToDrawingView(message)
                            }
                            else -> {
                                Timber.DebugTree().e(
                                    "DrawingActivity - Unknown motion event: ${
                                        message.motionEvent
                                    }"
                                )
                            }
                        }
                    }
                    is DrawAction -> {
                        // todo needed? currently server only sends the drawData to the non-drawing players
                        //    unless its updating the drawing player when rejoining the room.
                        //if(binding.drawingView.isEnabled) return@collect // only draw server data if this user is NOT drawing player

                        when(message.action) {
                            DRAW_ACTION_UNDO -> { binding.drawingView.undo() }
                            DRAW_ACTION_DRAW -> { /* do nothing */ }
                            DRAW_ACTION_ERASE -> { /* do nothing */ }
                            else -> {
                                Timber.DebugTree().e("DrawingActivity - Unexpected DrawAction action: ${message.action}")
                            }
                        }
                    }
                    is GameError -> {
                        when(message.errorType) {
                            GameError.ERROR_TYPE_ROOM_NOT_FOUND -> {
                                showSnackbar("Room not found - please try again, ${message.errorMessage}")
                                finish()
                            }
                        }
                    }
                    is Announcement -> {
                        addChatItemToChatMessagesAndScroll(message)
                    }
                    is ChatMessage -> {
                        addChatItemToChatMessagesAndScroll(message)
                    }
                    else -> {
                        Timber.e("DrawingActivity - socketBaseMessageEvent - Unexpected BaseMessage type: ${message.type}")
                    }
                }

            }
        }
    }

    private fun listenToSocketConnectionEvents() = lifecycleScope.launchWhenStarted {
        viewModel.socketConnectionEventChannel.collect  { event ->
            when(event) {
                is WebSocket.Event.OnConnectionOpened<*> -> {
                    viewModel.sendBaseMessageType(
                        JoinRoomHandshake(args.playerName, args.roomName, clientId)
                    )
                    viewModel.setConnectionProgressBarVisible(false)
                }
                is WebSocket.Event.OnConnectionClosed -> {
                    viewModel.setConnectionProgressBarVisible(false)
                }
                is WebSocket.Event.OnConnectionFailed -> {
                    viewModel.setConnectionProgressBarVisible(false)
                    showSnackbar(getString(R.string.connection_failed))

                    event.throwable.printStackTrace()
                }
                else -> {
                    Timber.e("DrawingActivity - socketConnectionEvent - Unexpected event type: ${event.javaClass}")
                }
            }
        }
    }

    /////////////////////////////////////
    // Lifecycle Handling              //
    /////////////////////////////////////

    // Lifecycle Observer
    // The reason we call this instead of onStop is because we are using the voice recording
    //   functionality and that requires the activity to be stopped while the permissions
    //   are being requested. This allows the game to continue while the permissions are
    //   being requested. This ON_STOP is only called when the activity is completely stopped.
    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)  // fired when app has been minimized
    private fun onAppInBackground() {
        viewModel.sendTemporaryDisconnectRequest()
    }

    // Back press to exit the room
    override fun onBackPressed() {
        LeaveDialog().apply {
            setPositiveClickListener {
                viewModel.sendPermanentDisconnectRequest {
                    viewModel.finalBackButtonPressed()
                }
                // finish() // don't call finish(), because this will close the app.
            }
        }.show(supportFragmentManager, "LeaveDialog")
    }

    override fun onPause() {
        super.onPause()

        // Save the state of the chat message recycler view
        binding.rvChat.layoutManager?.onSaveInstanceState()
    }

    override fun onDestroy() {
        super.onDestroy()
        speechRecognizer.destroy()
    }

    ////////////////////////////////////
    // Drawing View handling          //
    ////////////////////////////////////

    @SuppressLint("ClickableViewAccessibility")  // for onTouchListener not implementing performClick()
    private fun setupDrawingViewTouchListenerToSendDrawDataToServer(drawView: DrawingView) {

        // Listen to the touch events and send them to the server via sockets
        binding.drawingView.setOnTouchListener { _, event ->
            //println("isEnabled=${binding.drawingView.isEnabled}, $event")

            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    viewModel.sendBaseMessageType(
                        createDrawDataForServer(
                            event.x, event.y,
                            event.x, event.y,
                            DRAW_DATA_MOTION_EVENT_ACTION_DOWN
                        )
                    )
                }
                MotionEvent.ACTION_MOVE -> {
                    viewModel.sendBaseMessageType(
                        createDrawDataForServer(
                            drawView.getCurrentX(), drawView.getCurrentY(),
                            event.x, event.y,
                            DRAW_DATA_MOTION_EVENT_ACTION_MOVE
                        )
                    )
                }
                MotionEvent.ACTION_UP -> {
                    viewModel.sendBaseMessageType(
                        createDrawDataForServer(
                            drawView.getCurrentX(), drawView.getCurrentY(),
                            drawView.getCurrentX(), drawView.getCurrentY(),
                            DRAW_DATA_MOTION_EVENT_ACTION_UP
                        )
                    )
                }
            }

            false
        }
    }

    // Maps raw coordinates to relative coordinates (relative in the drawing view)
    private fun createDrawDataForServer(
        fromX: Float, fromY: Float,
        toX: Float,   toY: Float,
        motionEvent: String
    ): DrawData {
        return DrawData(
            roomName = args.roomName,
            color = binding.drawingView.getColor(),
            strokeWidth = binding.drawingView.getStrokeWidth(),
            fromX = fromX / binding.drawingView.getViewWidth(),
            fromY = fromY / binding.drawingView.getViewHeight(),
            toX = toX / binding.drawingView.getViewWidth(),
            toY = toY / binding.drawingView.getViewHeight(),
            motionEvent = motionEvent
        )
    }

    // Render the "Drawing Player" drawing from the server
    private fun renderDrawDataToDrawingView(drawData: DrawData) {

        // Converts DrawData from the server to DrawData mapped to the current DrawingView aspect ratio
        fun parseDrawData(drawData: DrawData): DrawData {
            return DrawData(
                roomName = drawData.roomName,
                color = drawData.color,
                strokeWidth = drawData.strokeWidth,
                fromX = drawData.fromX * binding.drawingView.getViewWidth(),
                fromY = drawData.fromY * binding.drawingView.getViewHeight(),
                toX = drawData.toX * binding.drawingView.getViewWidth(),
                toY = drawData.toY * binding.drawingView.getViewHeight(),
                motionEvent = drawData.motionEvent
            )
        }

        val drawDataParsed = parseDrawData(drawData)

        // Select the drawing color radio button for color selection
        selectColorRadioButtonForColor(drawDataParsed.color)

        when(drawDataParsed.motionEvent) {
            DRAW_DATA_MOTION_EVENT_ACTION_DOWN -> {
                binding.drawingView.startTouchExternally(
                    drawDataParsed.fromX,
                    drawDataParsed.fromY,
                    drawDataParsed.color,
                    drawDataParsed.strokeWidth
                )
            }
            DRAW_DATA_MOTION_EVENT_ACTION_MOVE -> {
                binding.drawingView.moveTouchExternally(
                    drawDataParsed.toX,
                    drawDataParsed.toY,
                    drawDataParsed.color,
                    drawDataParsed.strokeWidth
                )
            }
            DRAW_DATA_MOTION_EVENT_ACTION_UP -> {
                binding.drawingView.stopTouchExternally()
            }
            else -> {
                throw IllegalStateException("Unknown DRAW_DATA_MOTION_EVENT event type: ${drawDataParsed.motionEvent}")
            }
        }
    }

    // Select the drawing color of the drawing view
    private fun selectColor(color: ColorInt) {
        curDrawingColor = color

        binding.drawingView.setColor(color)

        // in case user has just used the eraser
        binding.drawingView.setStrokeWidth(Constants.DEFAULT_PAINT_STROKE_WIDTH)

        selectColorRadioButtonForColor(color) // update radio button selection
    }

    // Select the radio button for a given color
    private fun selectColorRadioButtonForColor(color: ColorInt) {
        binding.colorGroup.check(
            resourceColorToColorButtonIdMap[color] ?: R.id.rbBlack
        )
    }

    //  Map of Colors (ColorInt) to color radio button Id (ResId)
    private fun getColorToColorButtonIdMap(): Map<ColorInt, ResId> { // <@ColorInt, @IdRes>
        return mapOf(
              Color.RED     to R.id.rbRed,
              Color.GREEN   to R.id.rbGreen,
              Color.BLUE    to R.id.rbBlue,
              Color.GRAY    to R.id.rbGray,
              ContextCompat.getColor(this, android.R.color.holo_orange_dark)
                            to R.id.rbOrange,
              Color.WHITE   to R.id.rbEraser,
              Color.BLACK   to R.id.rbBlack,
        )
    }

    private fun setDrawingIsEnabledAndColorButtonsVisible(isEnabled: Boolean) {
        binding.colorGroup.isVisible = isEnabled
        binding.ibUndo.isVisible = isEnabled
        binding.ibUndo.isEnabled = isEnabled
        binding.drawingView.isEnabled = isEnabled
    }


    /////////////////////////////////
    // Chat Messages               //
    /////////////////////////////////

    private fun setupChatMessageRecyclerView() {
        binding.rvChat.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(this@DrawingActivity)
            rvChatMessageAdapter = ChatMessageAdapter(args.playerName, clientId)
            adapter = rvChatMessageAdapter
        }

        // Only restore the RV state when the list is empty
        rvChatMessageAdapter.stateRestorationPolicy =
            RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
    }

    private fun updateChatMessages(chatList: List<BaseMessageType>) {
        rvChatMessageAdapter.updateChatMessages(chatList, lifecycleScope) // replace at end todo
    }

    private suspend fun addChatItemToChatMessagesAndScroll(chatItem: BaseMessageType) {

        // Is the user scrolled to the bottom of the list?
        //   If so, we should scroll to reveal the new message.
        val isScrolledToBottom = !binding.rvChat.canScrollVertically(CAN_SCROLL_DOWN)

        // Add the chat item to the bottom of the chat messages list
        updateChatMessages(rvChatMessageAdapter.chatItems + chatItem)

        // wait for the update job to finish before (possibly) scrolling down to see the new message.
        rvChatMessageAdapter.waitForChatMessagesToUpdate()

        // Is the user scrolled to the bottom of the list? If so, scroll down to reveal new message.
        if (isScrolledToBottom) {
            // scroll to the last item
            binding.rvChat.scrollToPosition(rvChatMessageAdapter.chatItems.size - 1)
        }
    }

    private fun setChatMessageInputIsVisible(isVisible: Boolean) {
        binding.tilMessage.isVisible = isVisible
        binding.ibSend.isVisible = isVisible
        binding.ibClearText.isVisible = isVisible
        binding.ibMic.isVisible = isVisible
    }

    private fun sendSetWordToGuessMessage(word: String, roomName: String) {
        val setWordToGuess = SetWordToGuess(word, roomName)

        viewModel.sendBaseMessageType(setWordToGuess)
    }

    /////////////////////////////////////////
    // Players List in the Nav Drawer      //
    /////////////////////////////////////////

    private fun setupPlayerListInNavDrawer(navHeader: View) {
        // Setup the recycler view for the list of players
        //   note: because the PlayerAdapter constructor as no args, we have injected it (see @Inject)
        val rvPlayers = navHeader.findViewById<RecyclerView>(R.id.rvPlayers)

        rvPlayers.apply {
            layoutManager = LinearLayoutManager(this@DrawingActivity)
            adapter = rvPlayersAdapter
        }
    }

    private fun updatePlayersList(players: List<PlayerData>) {
        // Add the room name to the players list (hacky)
        val playersWithRoomNameFirst =
            listOf(PlayerData("Room: ${args.roomName}")) + players

        rvPlayersAdapter.updatePlayers(playersWithRoomNameFirst, lifecycleScope)
    }

    ///////////////////////////////////////////////////////////////////
    // Permissions Handling for recording audio & speech recognition //
    ///////////////////////////////////////////////////////////////////

    private fun isRecordAudioPermissionInManifest() =
        EasyPermissions.hasPermissions(
            this,
            Manifest.permission.RECORD_AUDIO
        )

    private fun requestRecordAudioPermission() {
        EasyPermissions.requestPermissions(
            this,
            getString(R.string.rationale_record_audio),  // shown when permission is denied the first time
            REQUEST_CODE_RECORD_AUDIO,
            Manifest.permission.RECORD_AUDIO
        )
    }

    /// permission receivers ///

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }

    override fun onPermissionsGranted(requestCode: Int, perms: MutableList<String>) {
        if (requestCode == REQUEST_CODE_RECORD_AUDIO) {
            binding.ibMic.isEnabled = true
            showToast("Audio record & speech recognition permission granted")
        }
    }

    // For Recording audio
    override fun onPermissionsDenied(requestCode: Int, perms: MutableList<String>) {
        if (requestCode == REQUEST_CODE_RECORD_AUDIO) {
            binding.ibMic.isEnabled = false

            if (EasyPermissions.somePermissionPermanentlyDenied(this, perms)) {
                // User has permanently denied the permission for audio recording.
                // show the user the AppSettingsDialog to go to settings and enable the permissions.
                AppSettingsDialog.Builder(this).build().show()
            } else {
                showToast("Audio record permission denied")
            }
        }
    }

    ///////////////////////////////////////////////////////////////////
    // Speech Recognizer Handling                                    //
    ///////////////////////////////////////////////////////////////////

    /// UI Handlers ///

    // User clicked the "Start Speech Recognition" button
    private fun setSpeechRecognizerIsListening(isListening: Boolean) {
       if(isListening) {
           binding.ibMic.setImageResource(R.drawable.ic_mic)
           speechRecognizer.startListening(speechRecognizerIntent)
       } else {
           binding.ibMic.setImageResource(R.drawable.ic_mic_off)
           binding.etMessage.hint = ""
           speechRecognizer.stopListening()
       }
    }

    /// Speech Recognizer receivers ///

    override fun onReadyForSpeech(params: Bundle?) {
        binding.etMessage.hint = getString(R.string.speech_recognizer_listening)
    }

    override fun onBeginningOfSpeech() {
        binding.etMessage.hint = getString(R.string.speech_recognizer_deciphering_words)
    }

    override fun onRmsChanged(rmsdB: Float) {
        /* do nothing */
    }

    override fun onBufferReceived(buffer: ByteArray?) {
        /* do nothing */
    }

    override fun onEndOfSpeech() {
        viewModel.stopSpeechRecognizerListening()
    }

    override fun onError(error: Int) {
        /* do nothing */
    }

    override fun onResults(results: Bundle?) {
        val strings = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        val wordsToGuess = strings?.get(0) ?: ""

        if (wordsToGuess == "")
            binding.etMessage.hint = getString(R.string.speech_recognizer_unknown_word)
        else {
            (binding.etMessage.text.toString() + " " + wordsToGuess).let {
                binding.etMessage.setText(it)
            }
        }

        speechRecognizer.stopListening()
        viewModel.stopSpeechRecognizerListening()
    }

    override fun onPartialResults(partialResults: Bundle?) {
        /* do nothing */
    }

    override fun onEvent(eventType: Int, params: Bundle?) {
        /* do nothing */
    }

    ///////////////////////////////////////////////////////////////////
    // UI Utils                                                      //
    ///////////////////////////////////////////////////////////////////

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun showSnackbar(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    }
}




































