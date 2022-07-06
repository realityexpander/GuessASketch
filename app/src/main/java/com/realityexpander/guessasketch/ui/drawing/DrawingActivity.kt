package com.realityexpander.guessasketch.ui.drawing

import android.graphics.Color
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.realityexpander.guessasketch.R
import com.realityexpander.guessasketch.databinding.ActivityDrawingBinding
import com.realityexpander.guessasketch.util.Constants
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect

@AndroidEntryPoint
class DrawingActivity: AppCompatActivity() {

    private lateinit var binding: ActivityDrawingBinding

    private val viewModel: DrawingViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDrawingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.colorGroup.setOnCheckedChangeListener { _, checkedId ->
            viewModel.checkRadioButton(checkedId)
        }

        subscribeToUiStateUpdates()
    }

    // subscribe to updates from the view model
    private fun subscribeToUiStateUpdates() {
        lifecycleScope.launchWhenStarted {

            // update the color of the paintbrush
            viewModel.selectedColorButtonId.collect { id ->
                binding.colorGroup.check(id)

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
}