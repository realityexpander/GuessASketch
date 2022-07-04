package com.realityexpander.guessasketch.ui.setup.fragments

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
import com.realityexpander.guessasketch.ui.setup.SetupViewModel
import com.realityexpander.guessasketch.ui.setup.SetupViewModel.SetupEvent
import com.realityexpander.guessasketch.util.Constants.SEARCH_TEXT_DEBOUNCE_DELAY_MILLIS
import com.realityexpander.guessasketch.util.navigateSafely
import com.realityexpander.guessasketch.util.snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SelectRoomFragment: Fragment(R.layout.fragment_select_room) {

    private var _binding: FragmentSelectRoomBinding? = null
    private val binding
        get() = _binding!!

    private val viewModel: SetupViewModel by activityViewModels()

    private val args: SelectRoomFragmentArgs by navArgs()

    @Inject
    lateinit var roomAdapter: RoomAdapter

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
//            findNavController().navigateSafely(
//                R.id.action_selectRoomFragment_to_createRoomFragment,
//                bundleOf("playerName" to args.playerName)
//            )
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

    private fun listenToEvents() = lifecycleScope.launchWhenStarted {
        viewModel.setupEvent.collect { event ->
            when (event) {
                is SetupEvent.ShowLoadingEvent -> {
                    binding.roomsProgressBar.isVisible = true
                }
                is SetupEvent.HideLoadingEvent -> {
                    binding.roomsProgressBar.isVisible = false
                }
                is SetupEvent.JoinRoomEvent -> {
                    findNavController().navigateSafely(
                        R.id.action_selectRoomFragment_to_drawingActivity,
                        bundleOf(
                            "playerName" to args.playerName,
                            "roomName" to event.roomName
                        )
                    )
                }
                is SetupEvent.NavigateToCreateRoomEvent -> {
                    findNavController().navigateSafely(
                        R.id.action_selectRoomFragment_to_createRoomFragment,
                        bundleOf(
                            "playerName" to args.playerName,
                        )
                    )
                }
                is SetupEvent.JoinRoomErrorEvent -> {
                    snackbar(event.errorMessage)
                }
                is SetupEvent.GetRoomErrorEvent -> {
                    binding.apply {
                        roomsProgressBar.isVisible = false
                        tvNoRoomsFound.isVisible = false
                        ivNoRoomsFound.isVisible = false
                    }
                    snackbar(event.errorMessage)
                }
                else -> {
                    // ignore
                    Unit
                }
            }
        }
    }

    private fun subscribeToObservers() = lifecycleScope.launchWhenStarted {
        viewModel.rooms.collect { roomEvent ->
            when(roomEvent) {
                is SetupEvent.ShowLoadingEvent -> {
                    binding.roomsProgressBar.isVisible = true
                }
                is SetupEvent.GetRoomEvent -> {
                    binding.roomsProgressBar.isVisible = false
                    val isEmpty = roomEvent.rooms.isEmpty()
                    binding.tvNoRoomsFound.isVisible = isEmpty
                    binding.ivNoRoomsFound.isVisible = isEmpty

                    lifecycleScope.launch {
                        roomAdapter.updateDataset(roomEvent.rooms)
                    }
                }
                is SetupEvent.GetRoomEmptyEvent -> {
                    binding.roomsProgressBar.isVisible = false
                    roomAdapter.updateDataset(emptyList())
                    binding.tvNoRoomsFound.isVisible = true
                    binding.ivNoRoomsFound.isVisible = true
                }
                else -> {
                    // do nothing
                    Unit
                }

            }
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}