package com.realityexpander.guessasketch.ui.drawing

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.core.view.isVisible
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.navigation.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.realityexpander.guessasketch.data.remote.ws.messageTypes.DrawAction.Companion.DRAW_ACTION_UNDO
import com.realityexpander.guessasketch.data.remote.ws.messageTypes.DrawData.Companion.DRAW_DATA_MOTION_EVENT_ACTION_DOWN
import com.realityexpander.guessasketch.R
import com.realityexpander.guessasketch.data.remote.ws.messageTypes.PlayerData
import com.realityexpander.guessasketch.data.remote.common.Room
import com.realityexpander.guessasketch.data.remote.ws.messageTypes.*
import com.realityexpander.guessasketch.data.remote.ws.messageTypes.DrawAction.Companion.DRAW_ACTION_DRAW
import com.realityexpander.guessasketch.data.remote.ws.messageTypes.DrawAction.Companion.DRAW_ACTION_ERASE
import com.realityexpander.guessasketch.data.remote.ws.messageTypes.DrawData.Companion.DRAW_DATA_MOTION_EVENT_ACTION_MOVE
import com.realityexpander.guessasketch.data.remote.ws.messageTypes.DrawData.Companion.DRAW_DATA_MOTION_EVENT_ACTION_UP
import com.realityexpander.guessasketch.databinding.ActivityDrawingBinding
import com.realityexpander.guessasketch.di.CLIENT_ID
import com.realityexpander.guessasketch.ui.adapters.ChatMessageAdapter
import com.realityexpander.guessasketch.ui.adapters.PlayerAdapter
import com.realityexpander.guessasketch.ui.views.DrawingView
import com.realityexpander.guessasketch.util.Constants
import com.realityexpander.guessasketch.util.hideKeyboard
import com.tinder.scarlet.WebSocket
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named

typealias ColorInt = Int // like @ColorInt
typealias ResId = Int // like @ResId

const val CAN_SCROLL_DOWN = 1 // List scroll position is NOT at the bottom, ie: Can scroll down if needed

@AndroidEntryPoint
class DrawingActivity: AppCompatActivity() {

    private lateinit var binding: ActivityDrawingBinding

    private val viewModel: DrawingViewModel by viewModels()
    private val args by navArgs<DrawingActivityArgs>()

    @Inject
    @Named(CLIENT_ID)
    lateinit var clientId: String

    private var isDrawingPlayer = false

    // Color radio buttons
    private lateinit var resourceColorToButtonIdMap: Map<Int, Int>
    private var curDrawingColor: Int = Color.BLACK

    // Chat message list
    private lateinit var rvChatMessageAdapter: ChatMessageAdapter

    // Drawer menu (List of players, rank, score)
    private lateinit var toggleDrawer: ActionBarDrawerToggle
    @Inject  // because the PlayerAdapter constructor as no args, we have injected it
    lateinit var rvPlayersAdapter: PlayerAdapter
    // private lateinit var rvPlayers: RecyclerView  // not needed?


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDrawingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel.playerName = args.playerName

        // Select the color of the drawing player's pen
        binding.colorGroup.setOnCheckedChangeListener { _, checkedId ->
            viewModel.selectColorRadioButton(checkedId)
        }

        // Setup the map of resource colors to radio button ids
        resourceColorToButtonIdMap = getColorToButtonIdMap()

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
            hideKeyboard(binding.root)
        }

        // Drawing Path listener
        binding.drawingView.setPathDataStackChangedListener { pathStack ->
            viewModel.setPathStackData(pathStack)
        }


        setupNavDrawer()
        setupChatMessageRecyclerView()

        subscribeToUiStateEvents()

        listenToSocketConnectionEvents()
        listenToSocketBaseMessageEvents()

        setupDrawingViewTouchListenerToSendDrawDataToServer(binding.drawingView)
    }

    override fun onPause() {
        super.onPause()

        // Save the state of the chat message recycler view
        binding.rvChat.layoutManager?.onSaveInstanceState()
    }

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

        // Setup the recycler view for the list of players
        //   note: because the PlayerAdapter constructor as no args, we have injected it (see @Inject)
        val rvPlayers = navHeader.findViewById<RecyclerView>(R.id.rvPlayers)
        rvPlayers.apply {
            layoutManager = LinearLayoutManager(this@DrawingActivity)
            adapter = rvPlayersAdapter
        }

    }

    // subscribe to UI state updates from the view model
    private fun subscribeToUiStateEvents() {
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
                curDrawingColor = resourceColorToButtonIdMap.entries.firstOrNull { colorToButtonId ->
                        colorToButtonId.value == buttonId
                    }?.key ?: Color.BLACK
                selectColor(curDrawingColor)

            }
        }

        // Connection Progress Bar
        lifecycleScope.launchWhenStarted {
            viewModel.connectionProgressBarVisible.collect { isVisible ->
                binding.connectionProgressBar.visibility = if (isVisible) View.VISIBLE else View.GONE
            }
        }

        // Pick Word Overlay visibility
        lifecycleScope.launchWhenStarted {
            viewModel.pickWordOverlayVisible.collect { isVisible ->
                binding.pickWordOverlay.visibility = if (isVisible) View.VISIBLE else View.GONE
            }
        }

        // Chat messages
        lifecycleScope.launchWhenStarted {
            viewModel.chatMessages.collect { chatItems ->
                if(rvChatMessageAdapter.chatItems.isEmpty()) {
                    updateChatMessagesList(chatItems)
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

        // Game Phase Update - update the round timer & Progress bar
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
                    // Gives the "word to guess" actual word to the drawing player.
                    // Server will send the "underscored" word to non-drawing players.
                    tvWordToGuessOrStatusMessage.text = gameState.wordToGuess

                    // Set these again (in case of config change)
                    isDrawingPlayer = gameState.drawingPlayerName == args.playerName  // todo check for clientId not playerName
                    setColorButtonGroupIsVisible(isDrawingPlayer) // only the drawingPlayer can change the drawing color
                    drawingView.isEnabled = isDrawingPlayer

                    ibMic.isVisible = !isDrawingPlayer // only the guessing players can use the mic

                    // NOTE: drawing player can still use the chat Messages
                    // todo should this be turned off for drawing player?
                    // setChatMessageInputIsVisible(!isDrawingPlayer)
                }
            }
        }

        // Game Phase Change (Updates are handled in gamePhaseTime)
        lifecycleScope.launchWhenStarted {
            viewModel.gamePhaseChange.collect { gamePhaseUpdate ->

                println("gamePhaseChange = $gamePhaseUpdate")

                when(gamePhaseUpdate.gamePhase) {
                    Room.GamePhase.INITIAL_STATE -> {
                        // do nothing

                        setColorButtonGroupIsVisible(false)
                        setChatMessageInputIsVisible(false)
                    }
                    Room.GamePhase.WAITING_FOR_PLAYERS -> {
                        binding.apply {
                            roundTimerProgressBar.isIndeterminate = true
                            tvWordToGuessOrStatusMessage.text = getString(R.string.waiting_for_players)
                        }
                        viewModel.cancelGamePhaseCountdownTimer()
                        viewModel.setConnectionProgressBarVisible(false)
                        setChatMessageInputIsVisible(true)
                    }
                    Room.GamePhase.WAITING_FOR_START -> {
                        binding.apply {
                            roundTimerProgressBar.max = gamePhaseUpdate.countdownTimerMillis.toInt()
                            roundTimerProgressBar.progress = roundTimerProgressBar.max
                            roundTimerProgressBar.isIndeterminate = false
                            tvWordToGuessOrStatusMessage.text = getString(R.string.waiting_for_start)
                            setChatMessageInputIsVisible(true)
                        }
                    }
                    Room.GamePhase.NEW_ROUND -> {
                        binding.apply {
                            roundTimerProgressBar.max = gamePhaseUpdate.countdownTimerMillis.toInt() // set the max value of the progress bar to the round time
                            gamePhaseUpdate.drawingPlayerName?.let { drawingPlayerName ->
                                tvWordToGuessOrStatusMessage.text = getString(R.string.drawing_player_is_choosing_word, drawingPlayerName)
                            }

                            drawingView.isEnabled = false // no one can draw while the word is being chosen
                            drawingView.clearDrawing() // clear the drawing view
                            selectColor(Color.BLACK) // reset drawing color to black

                            // Is this the drawing player? If yes, show the pick word overlay
                            isDrawingPlayer = gamePhaseUpdate.drawingPlayerName == args.playerName
                            viewModel.setPickWordOverlayVisible(isDrawingPlayer) // only the drawing player can choose a word

                            // disable "undo" for everyone
                            // (will be re-enabled when player is drawing player)
                            ibUndo.isEnabled = false

                            setChatMessageInputIsVisible(true)
                        }
                    }
                    Room.GamePhase.ROUND_IN_PROGRESS -> {
                        binding.apply {
                            roundTimerProgressBar.max = gamePhaseUpdate.countdownTimerMillis.toInt() // set the max value of the progress bar to the round time
                            viewModel.setPickWordOverlayVisible(false) // no one can pick the word anymore
                            setChatMessageInputIsVisible(true)

                            if (gamePhaseUpdate.drawingPlayerName == args.playerName) {
                                drawingView.isEnabled = true // only the drawingPlayer can draw
                                ibUndo.isEnabled = false // only the drawingPlayer can undo
                            }
                        }
                    }
                    Room.GamePhase.ROUND_ENDED -> {
                        binding.apply {
                            roundTimerProgressBar.max = gamePhaseUpdate.countdownTimerMillis.toInt() // set the max value of the progress bar to the round time
                            drawingView.isEnabled = false // no one can draw while the word is being shown
                            setColorButtonGroupIsVisible(false) // no one can change the drawing color
                            setChatMessageInputIsVisible(true)

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
                        Timber.DebugTree().e("DrawingActivity - Unexpected GamePhaseUpdate type: ${gamePhaseUpdate.gamePhase}")
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
    }

    private fun listenToSocketBaseMessageEvents() {
        lifecycleScope.launchWhenStarted {
            viewModel.socketBaseMessageEvent.collect { message ->

                when(message) {
                    is DrawData -> {
                        // only draw server data if this user is NOT the drawing player
                        if(binding.drawingView.isEnabled) return@collect

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
                        if(binding.drawingView.isEnabled) return@collect // only draw server data if this user is NOT drawing player

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
                        //showSnackbar("${message.message} - ${message.announcementType}")
                        addChatItemToChatMessagesAndScroll(message)
                    }
                    is ChatMessage -> {
                        addChatItemToChatMessagesAndScroll(message)
                    }
                    else -> {
                        Timber.DebugTree().e("DrawingActivity - Unexpected BaseMessage type: ${message.type}")
                    }
                }

            }
        }
    }

    private fun listenToSocketConnectionEvents() = lifecycleScope.launchWhenStarted {
        viewModel.socketConnectionEvent.collect  { event ->
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
                    // do nothing
                }
            }
        }
    }

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
            roomName = args.roomName ?: throw IllegalStateException("Room name is null"),
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
            resourceColorToButtonIdMap[color] ?: R.id.rbBlack
        )
    }

    //  Map of Colors to radio button IDs
    private fun getColorToButtonIdMap(): Map<ColorInt, ResId> { // <@ColorInt, @IdRes>
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

    private fun setColorButtonGroupIsVisible(isVisible: Boolean) {
        binding.colorGroup.isVisible = isVisible
        binding.ibUndo.isVisible = isVisible
    }

    private fun showSnackbar(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    }

    // For ActionBarDrawer  // todo is this used? remove?
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if(toggleDrawer.onOptionsItemSelected(item)) {
            return true
        }
        return super.onOptionsItemSelected(item)
    }

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

    // todo put this job in the adapter?
    private var updateChatMessagesJob: Job? = null // for cancelling the update job when new messages are received
    private fun updateChatMessagesList(chatList: List<BaseMessageType>) {
        updateChatMessagesJob?.cancel() // cancel the previous job if it exists
        updateChatMessagesJob = lifecycleScope.launch {
            rvChatMessageAdapter.updateDataset(chatList)
        }

        // Handle the job & cancellation in the RV (not here in the activity)
        // chatMessageAdapter.updateChatMessageList(chatList, lifecycleScope) // replace at end todo
    }

    // todo put this job in the adapter?
    private var updatePlayersListJob: Job? = null // for cancelling the update job when new messages are received
    private fun updatePlayersList(players: List<PlayerData>) {
        updatePlayersListJob?.cancel() // cancel the previous job if it exists
        updatePlayersListJob = lifecycleScope.launch {
            rvPlayersAdapter.updateDataset(players)
        }

        // Handle the job & cancellation in the RV (not here in the activity)
        // chatMessageAdapter.updateChatMessageList(chatList, lifecycleScope) // replace at end todo
    }

    private suspend fun addChatItemToChatMessagesAndScroll(chatItem: BaseMessageType) {

        // Is the user scrolled to the bottom of the list?
        //   If so, we should scroll to reveal the new message.
        val isScrolledToBottom = !binding.rvChat.canScrollVertically(CAN_SCROLL_DOWN)

        // Add the chat item to the bottom of the chat messages list
        updateChatMessagesList(rvChatMessageAdapter.chatItems + chatItem)

        // wait for the update job to finish before (possibly) scrolling down to see the new message.
        updateChatMessagesJob?.join()

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
    }

    private fun sendSetWordToGuessMessage(word: String, roomName: String) {
        val setWordToGuess = SetWordToGuess(word, roomName)

        viewModel.sendBaseMessageType(setWordToGuess)
    }
}




































