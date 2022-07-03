package com.realityexpander.guessasketch.ui.setup.fragments

import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.realityexpander.guessasketch.R
import com.realityexpander.guessasketch.databinding.FragmentUsernameBinding
import com.realityexpander.guessasketch.ui.setup.SetupViewModel
import dagger.hilt.android.AndroidEntryPoint
import com.realityexpander.guessasketch.ui.setup.SetupViewModel.SetupEvent.*
import com.realityexpander.guessasketch.util.Constants.MAX_PLAYER_NAME_LENGTH
import com.realityexpander.guessasketch.util.Constants.MIN_PLAYER_NAME_LENGTH
import com.realityexpander.guessasketch.util.navigateSafely
import com.realityexpander.guessasketch.util.snackbar
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.flow.collect

@AndroidEntryPoint
class UsernameFragment: Fragment(R.layout.fragment_username) {

    private var _binding: FragmentUsernameBinding? = null
    private val binding
        get() = _binding!!

    private val viewModel: SetupViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentUsernameBinding.bind(view)

        listenToEvents()

        binding.btnNext.setOnClickListener {
            viewModel.validatePlayerNameAndNavigateToSelectRoom(
                binding.etUsername.text.toString()
            )
        }
    }

    private fun listenToEvents() {
        lifecycleScope.launchWhenStarted {
            viewModel.setupEvent.collect { event ->
                when(event) {
                    is NavigateToSelectRoomEvent -> {
                        findNavController().navigateSafely(
                            R.id.action_usernameFragment_to_selectRoomFragment,
                            bundleOf(
                                "playerName" to event.playerName
                            )
                        )
//                        Buggy way
//                        findNavController().navigate(
//                            UsernameFragmentDirections.actionUsernameFragmentToSelectRoomFragment(
//                                event.playerName
//                            )
//                        )
                    }
                    InputEmptyError -> {
                        snackbar(R.string.error_field_empty)
                    }
                    InputTooLongError -> {
                        snackbar(R.string.error_room_name_too_long, value = MAX_PLAYER_NAME_LENGTH)
                    }
                    InputTooShortError -> {
                        snackbar(R.string.error_room_name_too_short, value = MIN_PLAYER_NAME_LENGTH)
                    }
                    else -> {
                        // Do nothing
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