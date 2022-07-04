package com.realityexpander.guessasketch.ui.setup.fragments

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import androidx.annotation.ArrayRes
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.realityexpander.guessasketch.R
import com.realityexpander.guessasketch.databinding.FragmentCreateRoomBinding
import com.realityexpander.guessasketch.ui.setup.SetupViewModel
import com.realityexpander.guessasketch.ui.setup.SetupViewModel.SetupEvent
import com.realityexpander.guessasketch.util.navigateSafely
import com.realityexpander.guessasketch.util.snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect

@AndroidEntryPoint
class CreateRoomFragment: Fragment(R.layout.fragment_create_room) {

    private var _binding: FragmentCreateRoomBinding? = null
    private val binding
        get() = _binding!!

    private val viewModel: SetupViewModel by viewModels()
    private val args: CreateRoomFragmentArgs by navArgs()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentCreateRoomBinding.bind(view)

        setupRoomSizeSpinner()

        binding.btnCreateRoom.setOnClickListener {
            viewModel.emitSetupEvent(SetupEvent.CreateRoomEvent)
        }

        listenToEvents()
    }

    private fun setupRoomSizeSpinner() {
        val roomSizes = resources.getStringArray(R.array.room_size_array)
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            roomSizes
        )
        binding.tvMaxPersons.setAdapter(adapter)
    }

    private fun listenToEvents() {
        lifecycleScope.launchWhenStarted {
            viewModel.setupEvent.collect { setupEvent ->
                when (setupEvent) {
                    is SetupEvent.CreateRoomEvent -> {
                        val roomName = binding.etRoomName.text.toString()
                        val maxPlayers = binding.tvMaxPersons.text.toString().toInt()

                        binding.createRoomProgressBar.isVisible = true
                        viewModel.createRoom(roomName, maxPlayers)
                    }
                    is SetupEvent.JoinRoomEvent -> {
                        findNavController().navigateSafely(
                            R.id.action_createRoomFragment_to_drawingActivity,
                            bundleOf(
                                "playerName" to args.playerName,
                                "roomName" to setupEvent.roomName
                            )
                        )
                    }
                    is SetupEvent.CreateRoomErrorEvent -> {
                        binding.createRoomProgressBar.isVisible = false
                        snackbar(setupEvent.errorMessage)
                    }
                    is SetupEvent.JoinRoomErrorEvent -> {
                        binding. createRoomProgressBar.isVisible = false
                        snackbar(setupEvent.errorMessage)
                    }
                    SetupEvent.HideLoadingEvent ->
                        binding.createRoomProgressBar.isVisible = false
                    SetupEvent.InputEmptyError ->
                        snackbar(getString(R.string.error_field_empty))
                    SetupEvent.InputTooLongError ->
                        snackbar(getString(R.string.error_room_name_too_long))
                    SetupEvent.InputTooShortError ->
                        snackbar(getString(R.string.error_room_name_too_short))
                    else -> {
                        // do nothing
                    }
                }
            }
        }
    }

    private fun getStringArrayItem(@ArrayRes stringArrayResId: Int, itemIndex: Int): String {
        return resources.getStringArray(stringArrayResId)[itemIndex]
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}