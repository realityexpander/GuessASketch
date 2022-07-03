package com.realityexpander.guessasketch.util

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore

val Context.dataStore by preferencesDataStore("settings")