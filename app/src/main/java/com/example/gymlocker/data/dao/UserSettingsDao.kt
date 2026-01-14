package com.example.gymlocker.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.gymlocker.data.entity.UserSettings
import kotlinx.coroutines.flow.Flow

@Dao
interface UserSettingsDao {

    @Query("SELECT * FROM user_settings WHERE userId = :userId LIMIT 1")
    fun observe(userId: Long): Flow<UserSettings?>

    @Query("SELECT * FROM user_settings WHERE userId = :userId LIMIT 1")
    suspend fun getOnce(userId: Long): UserSettings?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(settings: UserSettings)

    @Query("DELETE FROM user_settings WHERE userId = :userId")
    suspend fun deleteForUser(userId: Long): Int
}
