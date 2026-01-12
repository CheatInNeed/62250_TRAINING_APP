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

    @Query("SELECT * FROM users WHERE userId = :userId")
    fun getUser(userId: Long): Flow<User>

    @Query("SELECT * FROM users WHERE userId = :userId LIMIT 1")
    suspend fun getUserOnce(userId: Long): User?

    @Query("SELECT COUNT(*) FROM users")
    suspend fun countUsers(): Int

    // ✅ NEW: List profiles for the logged-in auth account
    @Query(
        """
        SELECT u.* FROM users u
        INNER JOIN auth_profiles ap ON ap.userId = u.userId
        WHERE ap.authId = :authId
        ORDER BY u.userId DESC
        """
    )
    fun observeProfilesForAuth(authId: Long): Flow<List<User>>
}
