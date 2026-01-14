package com.example.gymlocker.data.auth

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "session")

class SessionManager(private val context: Context) {

    private val KEY_LOGGED_IN = booleanPreferencesKey("logged_in")
    private val KEY_AUTH_ID = longPreferencesKey("auth_id")

    /**
     * Active profile (what the app is currently "acting as").
     */
    private val KEY_ACTIVE_PROFILE_USER_ID = longPreferencesKey("active_profile_user_id")

    /**
     * Last-used profile, persisted across logout/login.
     * We validate it belongs to the current authId before using it.
     */
    private val KEY_LAST_PROFILE_USER_ID = longPreferencesKey("last_profile_user_id")

    // ✅ Units (stored as strings)
    private val KEY_WEIGHT_UNIT = stringPreferencesKey("weight_unit") // "kg" | "lb"
    private val KEY_HEIGHT_UNIT = stringPreferencesKey("height_unit") // "cm" | "ft_in"

    val weightUnit: Flow<WeightUnit> = context.dataStore.data.map { prefs ->
        when (prefs[KEY_WEIGHT_UNIT] ?: "kg") {
            "lb" -> WeightUnit.LB
            else -> WeightUnit.KG
        }
    }

    val heightUnit: Flow<HeightUnit> = context.dataStore.data.map { prefs ->
        when (prefs[KEY_HEIGHT_UNIT] ?: "cm") {
            "ft_in" -> HeightUnit.FT_IN
            else -> HeightUnit.CM
        }
    }

    suspend fun setWeightUnit(unit: WeightUnit) {
        context.dataStore.edit { prefs ->
            prefs[KEY_WEIGHT_UNIT] = if (unit == WeightUnit.LB) "lb" else "kg"
        }
    }

    suspend fun setHeightUnit(unit: HeightUnit) {
        context.dataStore.edit { prefs ->
            prefs[KEY_HEIGHT_UNIT] = if (unit == HeightUnit.FT_IN) "ft_in" else "cm"
        }
    }

    val isLoggedIn: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_LOGGED_IN] ?: false
    }

    val authId: Flow<Long?> = context.dataStore.data.map { prefs ->
        prefs[KEY_AUTH_ID]
    }


    val activeProfileUserId: Flow<Long?> = context.dataStore.data.map { prefs ->
        prefs[KEY_ACTIVE_PROFILE_USER_ID]
    }

    val lastProfileUserId: Flow<Long?> = context.dataStore.data.map { prefs ->
        prefs[KEY_LAST_PROFILE_USER_ID]
    }

    suspend fun setLoggedIn(authId: Long) {
        context.dataStore.edit { prefs ->
            prefs[KEY_LOGGED_IN] = true
            prefs[KEY_AUTH_ID] = authId
            // do NOT set active profile here
        }
    }

    /**
     * Selecting a profile also updates "last used profile".
     */
    suspend fun setActiveProfile(profileUserId: Long) {
        context.dataStore.edit { prefs ->
            prefs[KEY_ACTIVE_PROFILE_USER_ID] = profileUserId
            prefs[KEY_LAST_PROFILE_USER_ID] = profileUserId
        }
    }

    suspend fun clearActiveProfile() {
        context.dataStore.edit { prefs ->
            prefs.remove(KEY_ACTIVE_PROFILE_USER_ID)
            // Keep LAST profile to support auto-select on next login
        }
    }

    /**
     * Logout clears auth + active profile, but keeps last_profile_user_id.
     */
    suspend fun clear() {
        context.dataStore.edit { prefs ->
            prefs.remove(KEY_LOGGED_IN)
            prefs.remove(KEY_AUTH_ID)
            prefs.remove(KEY_ACTIVE_PROFILE_USER_ID)
            // keep KEY_LAST_PROFILE_USER_ID
        }
    }

}
enum class WeightUnit { KG, LB }
enum class HeightUnit { CM, FT_IN }

