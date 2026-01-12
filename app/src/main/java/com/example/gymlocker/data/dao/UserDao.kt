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

    @Query("SELECT * FROM users WHERE userId = :userId LIMIT 1")
    fun getUser(userId: Long): Flow<User?>

    @Query("SELECT COUNT(*) FROM users")
    suspend fun countUsers(): Int

    // ✅ List all profiles belonging to one auth account
    @Query("SELECT * FROM users WHERE authOwnerId = :authId ORDER BY userId DESC")
    fun observeProfilesForAuth(authId: Long): Flow<List<User>>

    @Query("SELECT * FROM users WHERE authOwnerId = :authId ORDER BY userId DESC")
    suspend fun getProfilesForAuthOnce(authId: Long): List<User>
}
