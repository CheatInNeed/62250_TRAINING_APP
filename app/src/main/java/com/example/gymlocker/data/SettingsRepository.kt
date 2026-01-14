package com.example.gymlocker.data.repo

import android.content.Context
import com.example.gymlocker.data.auth.SessionManager
import com.example.gymlocker.data.database.AppDatabase
import com.example.gymlocker.data.entity.UserSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

class SettingsRepository(context: Context) {

    private val appContext = context.applicationContext
    private val db = AppDatabase.getDatabase(appContext)
    private val dao = db.userSettingsDao()
    private val session = SessionManager(appContext)

    /**
     * Always emits *something* when a profile is active:
     * - stored settings if present
     * - otherwise default UserSettings(userId)
     */
    val activeSettings: Flow<UserSettings?> =
        session.activeProfileUserId.flatMapLatest { userId ->
            if (userId == null) flowOf(null)
            else dao.observe(userId).map { it ?: UserSettings(userId = userId) }
        }

    suspend fun updateForActive(transform: (UserSettings) -> UserSettings) {
        val userId = session.activeProfileUserId.first() ?: return
        val current = dao.getOnce(userId) ?: UserSettings(userId = userId)
        dao.upsert(transform(current))
    }

    suspend fun setForActive(settings: UserSettings) {
        val userId = session.activeProfileUserId.first() ?: return
        dao.upsert(settings.copy(userId = userId))
    }
}
