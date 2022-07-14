package com.realityexpander.guessasketch.util

import android.app.Activity
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity

// Hide the keyboard (waaaaayyyy more complicated than it needs to be)
fun Activity.hideKeyboard(root: View) {
    val windowToken = root.windowToken
    val inputMethodManager = getSystemService(AppCompatActivity.INPUT_METHOD_SERVICE) as InputMethodManager
    windowToken?.let { wt ->
        inputMethodManager.hideSoftInputFromWindow(wt, 0)
    } ?: run {
        try {
            val keyboardHeight = InputMethodManager::class.java
                .getMethod("getInputMethodWindowVisibleHeight")
                .invoke(inputMethodManager) as Int
            if(keyboardHeight > 0) {
                inputMethodManager.toggleSoftInput(0, InputMethodManager.HIDE_NOT_ALWAYS)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}