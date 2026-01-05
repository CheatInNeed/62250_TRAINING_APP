package com.example.gymlocker.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.gymlocker.data.entity.MuscleGroup
import kotlinx.coroutines.flow.Flow

@Dao
interface MuscleGroupDao {
    @Insert
    suspend fun insert(muscleGroup: MuscleGroup): Long

    @Query("SELECT * FROM muscle_groups")
    fun getAllMuscleGroups(): Flow<List<MuscleGroup>>
}
