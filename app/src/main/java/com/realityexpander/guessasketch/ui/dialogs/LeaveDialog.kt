package com.realityexpander.guessasketch.ui.dialogs

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.realityexpander.guessasketch.R

class LeaveDialog: DialogFragment() {

    private var onPositiveClickListener: (() -> Unit)? = null
    fun setPositiveClickListener(listener: () -> Unit) {
        onPositiveClickListener = listener
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.dialog_leave_room_title)
            .setMessage(R.string.dialog_leave_room_message)
            .setPositiveButton("Yes") { _, _ ->
                onPositiveClickListener?.let { yes ->
                    yes()
                }
            }
            .setNegativeButton("No") { dialogInterface, _ ->
                dialogInterface?.cancel()
            }
            .create()


    }
}