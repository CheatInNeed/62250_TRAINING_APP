package com.example.gymlocker.data.auth

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "session")

class SessionManager(private val context: Context) {

    private val KEY_LOGGED_IN = booleanPreferencesKey("logged_in")
    private val KEY_AUTH_ID = longPreferencesKey("auth_id")
    private val KEY_PROFILE_USER_ID = longPreferencesKey("profile_user_id")

    val isLoggedIn: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_LOGGED_IN] ?: false
    }

    val profileUserId: Flow<Long?> = context.dataStore.data.map { prefs ->
        prefs[KEY_PROFILE_USER_ID]
    }

    suspend fun setLoggedIn(authId: Long, profileUserId: Long) {
        context.dataStore.edit { prefs ->
            prefs[KEY_LOGGED_IN] = true
            prefs[KEY_AUTH_ID] = authId
            prefs[KEY_PROFILE_USER_ID] = profileUserId
        }
    }

    suspend fun clear() {
        context.dataStore.edit { prefs ->
            prefs.remove(KEY_LOGGED_IN)
            prefs.remove(KEY_AUTH_ID)
            prefs.remove(KEY_PROFILE_USER_ID)
        }
    }
}
