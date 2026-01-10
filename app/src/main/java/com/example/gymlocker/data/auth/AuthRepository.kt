package com.example.gymlocker.data.auth

import android.content.Context
import com.example.gymlocker.data.database.AppDatabase
import com.example.gymlocker.data.entity.AuthAccount
import com.example.gymlocker.data.entity.User

class AuthRepository(context: Context) {

    private val appContext = context.applicationContext
    private val db = AppDatabase.getDatabase(appContext)
    private val userDao = db.userDao()
    private val authDao = db.authAccountDao()
    private val session = SessionManager(appContext)

    suspend fun register(email: String, password: String): Result<Unit> {
        val normalizedEmail = email.trim().lowercase()

        val existing = authDao.findByEmail(normalizedEmail)
        if (existing != null) {
            return Result.failure(IllegalStateException("Email is already registered"))
        }

        // Create default profile row (workouts attach to this User)
        // NOTE: your User requires name/height/weight, so we must supply values.
        val profileId = userDao.insert(
            User(
                name = "Default",
                height = 0,
                weight = 0
            )
        )

        val hash = PasswordHasher.sha256(password)

        val authId = authDao.insert(
            AuthAccount(
                email = normalizedEmail,
                passwordHash = hash,
                profileUserId = profileId
            )
        )

        session.setLoggedIn(authId, profileId)
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

        session.setLoggedIn(account.authId, account.profileUserId)
        return Result.success(Unit)
    }

    suspend fun logout() {
        session.clear()
    }

    fun isLoggedIn() = session.isLoggedIn
}
