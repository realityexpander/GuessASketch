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
import com.realityexpander.guessasketch.ui.views.DrawingView
import com.realityexpander.guessasketch.util.Constants
import com.tinder.scarlet.WebSocket
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import javax.inject.Inject
import javax.inject.Named

@AndroidEntryPoint
class DrawingActivity: AppCompatActivity() {

    private lateinit var binding: ActivityDrawingBinding

    private val viewModel: DrawingViewModel by viewModels()
    private val args by navArgs<DrawingActivityArgs>()

    @Inject
    @Named(CLIENT_ID)
    lateinit var clientId: String

    private lateinit var toggleDrawer: ActionBarDrawerToggle
    private lateinit var rvPlayers: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDrawingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.colorGroup.setOnCheckedChangeListener { _, checkedId ->
            viewModel.selectColorRadioButton(checkedId)
        }

        setupNavDrawer()

        subscribeToUiStateEvents()

        listenToSocketConnectionEvents()
        listenToSocketMessageEvents()

        setupDrawingViewTouchListenerToSendDrawDataToServer()

        viewModel.playerName = args.playerName
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

        lifecycleScope.launchWhenStarted {
            // Color of the paintbrush
            viewModel.selectedColorButtonId.collect { id ->
                binding.colorGroup.check(id)  // select the correct radio button

                when(id) {
                    R.id.rbRed -> selectColor(Color.RED)
                    R.id.rbBlue -> selectColor(Color.BLUE)
                    R.id.rbGreen -> selectColor(Color.GREEN)
                    R.id.rbYellow -> selectColor(Color.YELLOW)
                    R.id.rbOrange -> selectColor(
                        ContextCompat.getColor(this@DrawingActivity,
                            android.R.color.holo_orange_dark)
                    )
                    R.id.rbBlack -> selectColor(Color.BLACK)
                    R.id.rbEraser -> {
                        selectColor(Color.WHITE)
                        binding.drawingView.setStrokeWidth(40f)
                    }
                }
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
            // Connection Progress Bar
            viewModel.chooseWordOverlayVisible.collect { isVisible ->
                binding.chooseWordOverlay.visibility = if (isVisible) View.VISIBLE else View.GONE
            }
        }
    }

    private fun listenToSocketMessageEvents() {
        lifecycleScope.launchWhenStarted {
            viewModel.socketMessageEvent.collect { message ->

                when(message) {
                    is DrawData -> {
                        when(message.motionEvent) {
                            DRAW_DATA_MOTION_EVENT_ACTION_DOWN -> {
                                //binding.drawingView.startDrawing(message.x, message.y)
                            }
                            DRAW_DATA_MOTION_EVENT_ACTION_MOVE -> {

                            }
                            DRAW_DATA_MOTION_EVENT_ACTION_UP-> {

                            }
                            else -> {
                                //binding.drawingView.draw(message.x, message.y)
                            }
                        }
                    }
                    is DrawAction -> {
                        when(message.action) {
                            DRAW_ACTION_UNDO -> {} //binding.drawingView.undo()
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
                        println("Unknown/Unexpected message type: ${message.type}")
                    }
                }

            }
        }
    }

    private fun listenToSocketConnectionEvents() = lifecycleScope.launchWhenStarted {
        viewModel.socketConnectionEvent.collect  { event ->
            when(event) {
                is WebSocket.Event.OnConnectionOpened<*> -> {
                    viewModel.sendMessage(
                        // JoinRoomHandshake(args.playerName, args.roomName, dataStore.clientId()) // can also grab clientId directly
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

    private fun showSnackbar(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    }

    private fun selectColor(color: Int) {
        binding.drawingView.setColor(color)

        // in case user has just used the eraser
        binding.drawingView.setStrokeWidth(Constants.DEFAULT_PAINT_STROKE_WIDTH)
    }

    // For ActionBarDrawer
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if(toggleDrawer.onOptionsItemSelected(item)) {
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    @SuppressLint("ClickableViewAccessibility")  // for onTouchListener not implementing performClick()
    private fun setupDrawingViewTouchListenerToSendDrawDataToServer() {

        fun createDrawData(
            fromX: Float, fromY: Float,
            toX: Float,   toY: Float,
            motionEvent: Int
        ): DrawData {
            return DrawData(
                roomName = args.roomName ?: throw IllegalStateException("Room name is null"),
                color = binding.drawingView.getColor(),
                strokeWidth = binding.drawingView.getStrokeWidth(),
                fromX = fromX, fromY = fromY,
                toX = toX,     toY = toY,
                motionEvent = motionEvent
            )
        }

        // Listen to the touch events and send them to the server via sockets
        binding.drawingView.setOnTouchListener { view, event ->
            println("isEnabled=${binding.drawingView.isEnabled}, $event")
            val drawView = view as DrawingView

            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    viewModel.sendMessage(
                        createDrawData(
                            event.x, event.y,
                            event.x, event.y,
                            DRAW_DATA_MOTION_EVENT_ACTION_DOWN
                        )
                    )
                }
                MotionEvent.ACTION_MOVE -> {
                    viewModel.sendMessage(
                        createDrawData(
                            drawView.getCurrentX(), drawView.getCurrentY(),
                            event.x, event.y,
                            DRAW_DATA_MOTION_EVENT_ACTION_MOVE
                        )
                    )
                }
                MotionEvent.ACTION_UP -> {
                    viewModel.sendMessage(
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

}




































