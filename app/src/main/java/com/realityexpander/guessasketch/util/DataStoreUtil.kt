package com.realityexpander.guessasketch.util

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import java.util.*

// Create the datastore
val Context.dataStore by preferencesDataStore("settings")

// regular datastore save key, value pairs
// protobuf allows complex types, and type safe, but requires more work.

// Grab the persistent clientId from the DataStore
suspend fun DataStore<Preferences>.clientId(): String {
    val clientIdKey = stringPreferencesKey("clientId")
    val preferences = this.data.first()
    val clientIdExists = preferences[clientIdKey] != null

    return if(clientIdExists) {
        preferences[clientIdKey] ?: ""
    } else {
        val newClientId = UUID.randomUUID().toString()
        edit { settings ->
            settings[clientIdKey] = newClientId
        }
        newClientId
    }
}