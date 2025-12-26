package com.helldeck.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

/**
 * Shared preferences DataStore used across settings-like stores.
 */
val Context.helldeckDataStore: DataStore<Preferences> by preferencesDataStore(name = "helldeck_prefs")
