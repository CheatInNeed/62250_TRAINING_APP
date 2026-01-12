package com.example.gymlocker.data.auth

import android.content.Context
import com.example.gymlocker.data.database.AppDatabase
import com.example.gymlocker.data.entity.AuthAccount
import kotlinx.coroutines.flow.Flow

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
            AuthAccount(
                email = normalizedEmail,
                passwordHash = hash
            )
        )

        // ✅ login session
        session.setLoggedIn(authId)

        // ✅ try auto-select (likely none yet)
        autoSelectProfileIfPossible(authId)

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

        // ✅ login session
        session.setLoggedIn(account.authId)

        // ✅ Auto-select:
        // - if exactly 1 profile exists -> set it active
        // - otherwise leave null so UI can prompt user to create/pick
        autoSelectProfileIfPossible(account.authId)

        return Result.success(Unit)
    }

    private suspend fun autoSelectProfileIfPossible(authId: Long) {
        val profiles = userDao.getProfilesForAuthOnce(authId)
        if (profiles.size == 1) {
            session.setActiveProfile(profiles.first().userId)
        } else {
            // keep null (user must create or pick)
            session.clearActiveProfile()
        }
    }

    suspend fun logout() {
        session.clear()
    }

    fun isLoggedIn(): Flow<Boolean> = session.isLoggedIn
    fun activeProfileUserId(): Flow<Long?> = session.activeProfileUserId
    fun authId(): Flow<Long?> = session.authId

    suspend fun setActiveProfile(profileUserId: Long) {
        session.setActiveProfile(profileUserId)
    }

    suspend fun clearActiveProfile() {
        session.clearActiveProfile()
    }
}
