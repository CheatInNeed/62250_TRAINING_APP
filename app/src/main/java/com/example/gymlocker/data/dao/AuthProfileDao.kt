package com.example.gymlocker.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.gymlocker.data.entity.AuthProfile

@Dao
interface AuthProfileDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(link: AuthProfile): Long

    @Query("DELETE FROM auth_profiles WHERE authId = :authId AND userId = :userId")
    suspend fun delete(authId: Long, userId: Long)

    @Query("SELECT COUNT(*) FROM auth_profiles WHERE authId = :authId")
    suspend fun countProfilesForAuth(authId: Long): Int
}
