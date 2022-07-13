package com.realityexpander.guessasketch.ui.setup.select_room

import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.realityexpander.guessasketch.R
import com.realityexpander.guessasketch.databinding.FragmentSelectRoomBinding
import com.realityexpander.guessasketch.ui.adapters.RoomAdapter
import com.realityexpander.guessasketch.ui.setup.select_room.SelectRoomViewModel.SetupEvent
import com.realityexpander.guessasketch.ui.setup.select_room.SelectRoomViewModel.RoomsEvent
import com.realityexpander.guessasketch.util.Constants.SEARCH_TEXT_DEBOUNCE_DELAY_MILLIS
import com.realityexpander.guessasketch.util.navigateSafely
import com.realityexpander.guessasketch.util.snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint  // this is needed because of the @Inject of the RoomAdapter
class SelectRoomFragment: Fragment(R.layout.fragment_select_room) {

    private var _binding: FragmentSelectRoomBinding? = null
    private val binding
        get() = _binding!!

    private val viewModel: SelectRoomViewModel by activityViewModels()

    private val args: SelectRoomFragmentArgs by navArgs()

    @Inject
    lateinit var roomAdapter: RoomAdapter

    // Prevent crashing bug with recycler view
    private var updateRoomsJob: Job? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentSelectRoomBinding.bind(view)

        setupRecyclerView()
        roomAdapter.setOnRoomItemClickListener { room ->
            viewModel.joinRoom(args.playerName, room.roomName)
        }

        // Setup reload button
        binding.ibReload.setOnClickListener {
            binding.roomsProgressBar.isVisible = true
            binding.ivNoRoomsFound.isVisible = false
            binding.tvNoRoomsFound.isVisible = false
            viewModel.getRooms(binding.etRoomName.text.toString())
        }

        // Setup search bar
        var searchJob: Job? = null
        binding.etRoomName.addTextChangedListener { searchText ->
            searchJob?.cancel()
            searchJob = lifecycleScope.launch {
                delay(SEARCH_TEXT_DEBOUNCE_DELAY_MILLIS)
                viewModel.getRooms(searchText.toString())
            }
        }

        // Setup CreateRoom button
        binding.btnCreateRoom.setOnClickListener {
            viewModel.emitSetupEvent(SetupEvent.NavigateToCreateRoomEvent(args.playerName))
        }

        subscribeToObservers()
        listenToEvents()
        viewModel.getRooms("")
    }


    private fun setupRecyclerView() {
        binding.rvRooms.apply {
            adapter = roomAdapter
            layoutManager = LinearLayoutManager(context)
        }
    }

    // Listen to the UI Events
    private fun listenToEvents() = lifecycleScope.launchWhenStarted {
        viewModel.setupEvent.collect { setupEvent ->
            when (setupEvent) {
                is SetupEvent.JoinRoomEvent -> {
                    findNavController().navigateSafely(
                        R.id.action_selectRoomFragment_to_drawingActivity,
                        bundleOf(
                            "playerName" to args.playerName,
                            "roomName" to setupEvent.roomName
                        )
                    )
                }
                is SetupEvent.JoinRoomErrorEvent -> {
                    snackbar(setupEvent.errorMessage)
                }
                is SetupEvent.NavigateToCreateRoomEvent -> {
                    findNavController().navigateSafely(
                        R.id.action_selectRoomFragment_to_createRoomFragment,
                        bundleOf(
                            "playerName" to args.playerName,
                        )
                    )
                }
                else -> {
                    // do nothing
                }
            }
        }
    }

    // Track the room list changes
    private fun subscribeToObservers() = lifecycleScope.launchWhenStarted {
        viewModel.rooms.collect { roomsEvent ->
            when(roomsEvent) {
                is RoomsEvent.ShowLoadingEvent -> {
                    binding.roomsProgressBar.isVisible = true
                }
                is RoomsEvent.GetRoomsEvent -> {
                    binding.roomsProgressBar.isVisible = false
                    val isEmpty = roomsEvent.rooms.isEmpty()
                    binding.tvNoRoomsFound.isVisible = isEmpty
                    binding.ivNoRoomsFound.isVisible = isEmpty

                    updateRoomsJob?.cancel()  // Prevents a crash when updating the recyclerview
                    updateRoomsJob = lifecycleScope.launch {
                        roomAdapter.updateDataset(roomsEvent.rooms)
                    }
                }
                is RoomsEvent.GetRoomsEmptyEvent -> {
                    binding.roomsProgressBar.isVisible = false
                    roomAdapter.updateDataset(emptyList())
                    binding.tvNoRoomsFound.isVisible = true
                    binding.ivNoRoomsFound.isVisible = true
                }
                is RoomsEvent.GetRoomsErrorEvent -> {
                    binding.roomsProgressBar.isVisible = false
                    binding.tvNoRoomsFound.isVisible = false
                    binding.ivNoRoomsFound.isVisible = false
                    snackbar(roomsEvent.errorMessage)
                }
                is RoomsEvent.InitialState -> {
                    binding.roomsProgressBar.isVisible = false
                }

            }
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}