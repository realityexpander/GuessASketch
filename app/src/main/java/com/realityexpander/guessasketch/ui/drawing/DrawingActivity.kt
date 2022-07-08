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
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.navigation.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.realityexpander.guessasketch.data.remote.ws.messageTypes.DrawAction.Companion.DRAW_ACTION_UNDO
import com.realityexpander.guessasketch.data.remote.ws.messageTypes.DrawData.Companion.DRAW_DATA_MOTION_EVENT_ACTION_DOWN
import com.realityexpander.guessasketch.R
import com.realityexpander.guessasketch.data.remote.ws.messageTypes.*
import com.realityexpander.guessasketch.data.remote.ws.messageTypes.DrawAction.Companion.DRAW_ACTION_DRAW
import com.realityexpander.guessasketch.data.remote.ws.messageTypes.DrawAction.Companion.DRAW_ACTION_ERASE
import com.realityexpander.guessasketch.data.remote.ws.messageTypes.DrawData.Companion.DRAW_DATA_MOTION_EVENT_ACTION_MOVE
import com.realityexpander.guessasketch.data.remote.ws.messageTypes.DrawData.Companion.DRAW_DATA_MOTION_EVENT_ACTION_UP
import com.realityexpander.guessasketch.databinding.ActivityDrawingBinding
import com.realityexpander.guessasketch.di.CLIENT_ID
import com.realityexpander.guessasketch.ui.adapters.ChatMessageAdapter
import com.realityexpander.guessasketch.ui.views.DrawingView
import com.realityexpander.guessasketch.util.Constants
import com.tinder.scarlet.WebSocket
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named

typealias ColorInt = Int // like @ColorInt
typealias ResId = Int // like @ResId

@AndroidEntryPoint
class DrawingActivity: AppCompatActivity() {

    private lateinit var binding: ActivityDrawingBinding

    private val viewModel: DrawingViewModel by viewModels()
    private val args by navArgs<DrawingActivityArgs>()

    private lateinit var resourceColorToButtonIdMap: Map<Int, Int>

    private var curDrawingColor: Int = Color.BLACK

    private lateinit var chatMessageAdapter: ChatMessageAdapter

    @Inject
    @Named(CLIENT_ID)
    lateinit var clientId: String

    private lateinit var toggleDrawer: ActionBarDrawerToggle
    private lateinit var rvPlayers: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDrawingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel.playerName = args.playerName

//        // "test" playerName is the drawing player - for testing -- remove todo
//        binding.drawingView.isEnabled = args.playerName == "test"

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

        setupNavDrawer()
        setupChatMessageRecyclerView()

        subscribeToUiStateEvents()

        listenToSocketConnectionEvents()
        listenToSocketMessageEvents()

        setupDrawingViewTouchListenerToSendDrawDataToServer(binding.drawingView)
    }

    // Setup the drawer for the recyclerview list of players
    private fun setupNavDrawer() {
        toggleDrawer = ActionBarDrawerToggle(this, binding.root, R.string.open, R.string.close)
        toggleDrawer.syncState()
        val navHeader = layoutInflater.inflate(R.layout.nav_drawer_header, binding.navView)
        rvPlayers = navHeader.findViewById(R.id.rvPlayers)
        binding.root.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED) // only can be opened by clicking the "players" button

        binding.ibPlayers.setOnClickListener {
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

        // Choose Word Overlay visibility
        lifecycleScope.launchWhenStarted {
            viewModel.chooseWordOverlayVisible.collect { isVisible ->
                binding.chooseWordOverlay.visibility = if (isVisible) View.VISIBLE else View.GONE
            }
        }
    }

    // Listen to socket messages from the server
    private fun listenToSocketMessageEvents() {
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
                            DRAW_ACTION_DRAW -> {} //binding.drawingView.undo()
                            DRAW_ACTION_ERASE -> {} //binding.drawingView.undo()
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
                        showSnackbar("${message.message} - ${message.announcementType}")
                    }
                    else -> {
                        println("Unknown/Unexpected Socket Message type: ${message.type}")
                    }
                }

            }
        }
    }

    // Listen to socket connection events
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

    // Select the drawing color of the drawing view
    private fun selectColor(color: Int) {
        binding.drawingView.setColor(color)

        // in case user has just used the eraser
        binding.drawingView.setStrokeWidth(Constants.DEFAULT_PAINT_STROKE_WIDTH)
    }

    @SuppressLint("ClickableViewAccessibility")  // for onTouchListener not implementing performClick()
    private fun setupDrawingViewTouchListenerToSendDrawDataToServer(drawView: DrawingView) {

        // Maps raw coordinates to relative coordinates (relative in the drawing view)
        fun createDrawData(
            fromX: Float, fromY: Float,
            toX: Float,   toY: Float,
            motionEvent: String
        ): DrawData {
            return DrawData(
                roomName = args.roomName ?: throw IllegalStateException("Room name is null"),
                color = binding.drawingView.getColor(),
                strokeWidth = drawView.getStrokeWidth(),
                fromX = fromX / drawView.getViewWidth(),
                fromY = fromY / drawView.getViewHeight(),
                toX = toX / drawView.getViewWidth(),
                toY = toY / drawView.getViewHeight(),
                motionEvent = motionEvent
            )
        }

        // Listen to the touch events and send them to the server via sockets
        binding.drawingView.setOnTouchListener { _, event ->
            // println("isEnabled=${binding.drawingView.isEnabled}, $event")

            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    viewModel.sendBaseMessageType(
                        createDrawData(
                            event.x, event.y,
                            event.x, event.y,
                            DRAW_DATA_MOTION_EVENT_ACTION_DOWN
                        )
                    )
                }
                MotionEvent.ACTION_MOVE -> {
                    viewModel.sendBaseMessageType(
                        createDrawData(
                            drawView.getCurrentX(), drawView.getCurrentY(),
                            event.x, event.y,
                            DRAW_DATA_MOTION_EVENT_ACTION_MOVE
                        )
                    )
                }
                MotionEvent.ACTION_UP -> {
                    viewModel.sendBaseMessageType(
                        createDrawData(
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
        binding.colorGroup.check(
            resourceColorToButtonIdMap[drawDataParsed.color] ?: R.id.rbBlack
        )

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
                throw IllegalStateException("Unknown motion event type: ${drawDataParsed.motionEvent}")
            }
        }
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

    private fun showSnackbar(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    }

    // For ActionBarDrawer
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
            chatMessageAdapter = ChatMessageAdapter(args.playerName, clientId)
            adapter = chatMessageAdapter
        }
    }

}




































