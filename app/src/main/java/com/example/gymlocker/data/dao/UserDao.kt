package com.example.gymlocker.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.gymlocker.data.entity.User
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {

    // IMPORTANT: must return Long so we can link AuthAccount -> User(profile)
    @Insert
    suspend fun insert(user: User): Long

    @Query("SELECT * FROM users WHERE userId = :userId")
    fun getUser(userId: Long): Flow<User>

    @Query("SELECT COUNT(*) FROM users")
    suspend fun countUsers(): Int
}
