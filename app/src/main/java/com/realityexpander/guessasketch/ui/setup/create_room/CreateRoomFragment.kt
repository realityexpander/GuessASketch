package com.realityexpander.guessasketch.ui.setup.create_room

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.realityexpander.guessasketch.R
import com.realityexpander.guessasketch.databinding.FragmentCreateRoomBinding
import com.realityexpander.guessasketch.ui.common.Constants.MAX_ROOM_NAME_LENGTH
import com.realityexpander.guessasketch.ui.common.Constants.MIN_ROOM_NAME_LENGTH
import com.realityexpander.guessasketch.ui.setup.create_room.CreateRoomViewModel.SetupEvent
import com.realityexpander.guessasketch.util.hideKeyboard
import com.realityexpander.guessasketch.util.navigateSafely
import com.realityexpander.guessasketch.util.snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect

@AndroidEntryPoint  // is this needed since nothing is injected?
class CreateRoomFragment: Fragment(R.layout.fragment_create_room) {

    private var _binding: FragmentCreateRoomBinding? = null
    private val binding
        get() = _binding!!

    private val viewModel: CreateRoomViewModel by viewModels()
    private val args: CreateRoomFragmentArgs by navArgs()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentCreateRoomBinding.bind(view)

        //requireActivity().window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)

        setupRoomSizeSpinner()

        // Create room button
        binding.btnCreateRoom.setOnClickListener {
            viewModel.emitSetupEvent(SetupEvent.CreateRoomEvent)

            requireActivity().hideKeyboard(binding.root)
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
                        binding.createRoomProgressBar.isVisible = false
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
                    SetupEvent.InputEmptyError -> {
                        binding.createRoomProgressBar.isVisible = false
                        snackbar(getString(R.string.error_field_empty))
                    }
                    SetupEvent.InputTooLongError -> {
                        binding.createRoomProgressBar.isVisible = false
                        snackbar(R.string.error_room_name_too_long, MAX_ROOM_NAME_LENGTH)
                    }
                    SetupEvent.InputTooShortError -> {
                        binding.createRoomProgressBar.isVisible = false
                        snackbar(R.string.error_room_name_too_short, MIN_ROOM_NAME_LENGTH)
                    }
                    SetupEvent.HideLoadingEvent ->
                        binding.createRoomProgressBar.isVisible = false
                    else -> {
                        // do nothing
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}