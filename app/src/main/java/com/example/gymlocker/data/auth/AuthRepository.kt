package com.example.gymlocker.data.auth

import android.content.Context
import com.example.gymlocker.data.database.AppDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class AuthRepository(context: Context) {

    private val appContext = context.applicationContext
    private val db = AppDatabase.getDatabase(appContext)

    private val authDao = db.authAccountDao()
    private val userDao = db.userDao()

    private val session = SessionManager(appContext)

    suspend fun register(email: String, password: String): Result<Unit> {
        val normalizedEmail = email.trim().lowercase()

        val existing = authDao.findByEmail(normalizedEmail)
        if (existing != null) {
            return Result.failure(IllegalStateException("Email is already registered"))
        }

        val hash = PasswordHasher.sha256(password)

        val authId = authDao.insert(
            com.example.gymlocker.data.entity.AuthAccount(
                email = normalizedEmail,
                passwordHash = hash
            )
        )

        // Logged in, but no profile yet.
        session.setLoggedIn(authId)
        session.clearActiveProfile()

        return Result.success(Unit)
    }

    suspend fun login(email: String, password: String): Result<Unit> {
        val normalizedEmail = email.trim().lowercase()

        val account = authDao.findByEmail(normalizedEmail)
            ?: return Result.failure(IllegalArgumentException("Incorrect email or password"))

        val hash = PasswordHasher.sha256(password)
        if (hash != account.passwordHash) {
            return Result.failure(IllegalArgumentException("Incorrect email or password"))
        }

        // 1) set auth session
        session.setLoggedIn(account.authId)

        // 2) try auto-select last used profile (if it belongs to this auth)
        val last = session.lastProfileUserId.first()

        val selectedProfileId: Long? = when {
            last != null && userDao.belongsToAuth(last, account.authId) > 0 -> last
            else -> {
                // fallback: first profile for this auth (if any)
                userDao.listProfilesForAuth(account.authId).firstOrNull()?.userId
            }
        }

        if (selectedProfileId != null) {
            session.setActiveProfile(selectedProfileId)
        } else {
            session.clearActiveProfile()
        }

        return Result.success(Unit)
    }

    suspend fun logout() {
        session.clear()
    }

    fun isLoggedIn(): Flow<Boolean> = session.isLoggedIn

    fun activeProfileUserId(): Flow<Long?> = session.activeProfileUserId

    suspend fun setActiveProfile(profileUserId: Long) {
        session.setActiveProfile(profileUserId)
    }

    suspend fun clearActiveProfile() {
        session.clearActiveProfile()
    }
}
