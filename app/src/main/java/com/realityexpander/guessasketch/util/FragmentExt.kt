package com.realityexpander.guessasketch.util

import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar

fun Fragment.snackbar(message: String, duration: Int = Snackbar.LENGTH_LONG) {
        Snackbar.make(requireView(), message, duration).show()
}

fun Fragment.snackbar(@StringRes res: Int, value: Int? = null, duration: Int = Snackbar.LENGTH_LONG) {
    Snackbar.make(requireView(), getString(res, value), duration).show()
}