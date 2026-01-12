package com.example.gymlocker.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.gymlocker.data.entity.User
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {

    @Insert
    suspend fun insert(user: User): Long

    /**
     * Active profile lookup.
     * IMPORTANT: should return nullable if the row might not exist.
     */
    @Query("SELECT * FROM users WHERE userId = :userId LIMIT 1")
    fun getUser(userId: Long): Flow<User?>

    @Query("SELECT COUNT(*) FROM users")
    suspend fun countUsers(): Int

    /**
     * All profiles belonging to this auth account.
     */
    @Query("SELECT * FROM users WHERE authOwnerId = :authId ORDER BY userId ASC")
    fun observeProfilesForAuth(authId: Long): Flow<List<User>>

    /**
     * One-shot version (used at login for auto-select).
     */
    @Query("SELECT * FROM users WHERE authOwnerId = :authId ORDER BY userId ASC")
    suspend fun listProfilesForAuth(authId: Long): List<User>

    /**
     * Validate that a profile belongs to this auth account.
     */
    @Query("SELECT COUNT(*) FROM users WHERE userId = :userId AND authOwnerId = :authId")
    suspend fun belongsToAuth(userId: Long, authId: Long): Int
}
