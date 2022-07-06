package com.realityexpander.guessasketch.ui.drawing

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.MenuItem
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.realityexpander.guessasketch.R
import com.realityexpander.guessasketch.databinding.ActivityDrawingBinding
import com.realityexpander.guessasketch.util.Constants
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect

@AndroidEntryPoint
class DrawingActivity: AppCompatActivity() {

    private lateinit var binding: ActivityDrawingBinding

    private val viewModel: DrawingViewModel by viewModels()

    private lateinit var toggleDrawer: ActionBarDrawerToggle
    private lateinit var rvPlayers: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDrawingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.colorGroup.setOnCheckedChangeListener { _, checkedId ->
            viewModel.checkRadioButton(checkedId)
        }

        setupNavDrawer()

        subscribeToUiStateUpdates()
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

    // subscribe to updates from the view model
    private fun subscribeToUiStateUpdates() {
        lifecycleScope.launchWhenStarted {

            // update the color of the paintbrush
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
                        binding.drawingView.setThickness(40f)
                    }
                }
            }
        }
    }

    private fun selectColor(color: Int) {
        binding.drawingView.setColor(color)

        // in case user has just used the eraser
        binding.drawingView.setThickness(Constants.DEFAULT_PAINT_STROKE_WIDTH)
    }

    // For ActionBarDrawer
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if(toggleDrawer.onOptionsItemSelected(item)) {
            return true
        }
        return super.onOptionsItemSelected(item)
    }

}