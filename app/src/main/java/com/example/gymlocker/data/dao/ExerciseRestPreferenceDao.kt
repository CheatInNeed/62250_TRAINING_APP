package com.example.gymlocker.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.gymlocker.data.entity.ExerciseRestPreference

@Dao
interface ExerciseRestPreferenceDao {

    @Query(
        "SELECT restSeconds FROM exercise_rest_preference WHERE userId = :userId AND exerciseId = :exerciseId LIMIT 1"
    )
    suspend fun getRestSeconds(userId: Long, exerciseId: Long): Int?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(pref: ExerciseRestPreference)

    @Query("DELETE FROM exercise_rest_preference WHERE userId = :userId AND exerciseId = :exerciseId")
    suspend fun delete(userId: Long, exerciseId: Long)

    // ✅ NEW: needed for "Delete profile"
    @Query("DELETE FROM exercise_rest_preference WHERE userId = :userId")
    suspend fun deleteAllForUser(userId: Long): Int

}
