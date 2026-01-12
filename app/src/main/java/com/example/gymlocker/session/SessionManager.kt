package com.example.gymlocker.data.auth

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Top-level extension (must be outside the class)
private val Context.dataStore by preferencesDataStore(name = "session")

class SessionManager(private val context: Context) {

    private val KEY_LOGGED_IN = booleanPreferencesKey("logged_in")
    private val KEY_AUTH_ID = longPreferencesKey("auth_id")

    /**
     * Active profile (what the app is currently "acting as").
     * This supports: 1 auth account -> many profiles.
     */
    private val KEY_ACTIVE_PROFILE_USER_ID = longPreferencesKey("active_profile_user_id")

    val isLoggedIn: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_LOGGED_IN] ?: false
    }

    val authId: Flow<Long?> = context.dataStore.data.map { prefs ->
        prefs[KEY_AUTH_ID]
    }

    val activeProfileUserId: Flow<Long?> = context.dataStore.data.map { prefs ->
        prefs[KEY_ACTIVE_PROFILE_USER_ID]
    }

    /**
     * Login now only sets auth session — NOT a profile.
     */
    suspend fun setLoggedIn(authId: Long) {
        context.dataStore.edit { prefs ->
            prefs[KEY_LOGGED_IN] = true
            prefs[KEY_AUTH_ID] = authId
            // do NOT set active profile here
        }
    }

    suspend fun setActiveProfile(profileUserId: Long) {
        context.dataStore.edit { prefs ->
            prefs[KEY_ACTIVE_PROFILE_USER_ID] = profileUserId
        }
    }

    suspend fun clearActiveProfile() {
        context.dataStore.edit { prefs ->
            prefs.remove(KEY_ACTIVE_PROFILE_USER_ID)
        }
    }

    suspend fun clear() {
        context.dataStore.edit { prefs ->
            prefs.remove(KEY_LOGGED_IN)
            prefs.remove(KEY_AUTH_ID)
            prefs.remove(KEY_ACTIVE_PROFILE_USER_ID)
        }
    }
}
