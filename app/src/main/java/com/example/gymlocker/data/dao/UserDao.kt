package com.example.gymlocker.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.gymlocker.data.entity.User
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {

    @Insert
    suspend fun insert(user: User): Long

    @Query("SELECT * FROM users WHERE userId = :userId LIMIT 1")
    fun getUser(userId: Long): Flow<User?>

    @Query("SELECT * FROM users WHERE userId = :userId LIMIT 1")
    suspend fun getUserOnce(userId: Long): User?

    @Query("SELECT COUNT(*) FROM users")
    suspend fun countUsers(): Int

    @Query("SELECT * FROM users WHERE authOwnerId = :authId ORDER BY userId ASC")
    fun observeProfilesForAuth(authId: Long): Flow<List<User>>

    @Query("SELECT * FROM users WHERE authOwnerId = :authId ORDER BY userId ASC")
    suspend fun listProfilesForAuth(authId: Long): List<User>

    @Query("SELECT COUNT(*) FROM users WHERE userId = :userId AND authOwnerId = :authId")
    suspend fun belongsToAuth(userId: Long, authId: Long): Int

    @Update
    suspend fun update(user: User): Int

    @Query("UPDATE users SET name = :name, height = :height, weight = :weight WHERE userId = :userId")
    suspend fun updateBasics(userId: Long, name: String, height: Int, weight: Int): Int

    @Query("UPDATE users SET name = :defaultName, height = 0, weight = 0 WHERE userId = :userId")
    suspend fun resetBasics(userId: Long, defaultName: String = "User"): Int

    // ✅ NEW: delete a profile (workouts/templates should cascade via FK)
    @Query("DELETE FROM users WHERE userId = :userId")
    suspend fun deleteById(userId: Long): Int
}
