package com.example.gymlocker.data.auth

import android.content.Context
import com.example.gymlocker.data.database.AppDatabase
import com.example.gymlocker.data.entity.AuthAccount
import kotlinx.coroutines.flow.Flow

class AuthRepository(context: Context) {

    private val appContext = context.applicationContext
    private val db = AppDatabase.getDatabase(appContext)
    private val authDao = db.authAccountDao()
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

        // ✅ logged in, but NO profile selected/created yet
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

        // ✅ logged in, but NO profile selected/created yet
        session.setLoggedIn(account.authId)
        session.clearActiveProfile()

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
