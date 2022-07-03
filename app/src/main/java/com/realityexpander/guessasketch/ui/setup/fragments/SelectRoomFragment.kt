package com.realityexpander.guessasketch.ui.setup.fragments

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.realityexpander.guessasketch.R
import com.realityexpander.guessasketch.databinding.FragmentSelectRoomBinding
import com.realityexpander.guessasketch.databinding.FragmentUsernameBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SelectRoomFragment: Fragment(R.layout.fragment_select_room) {

    private var _binding: FragmentSelectRoomBinding? = null
    private val binding
        get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentSelectRoomBinding.bind(view)
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}