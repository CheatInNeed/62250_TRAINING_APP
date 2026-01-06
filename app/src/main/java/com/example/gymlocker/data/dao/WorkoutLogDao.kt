package com.example.gymlocker.data.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import com.example.gymlocker.data.entity.ExerciseLogWithSets
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutLogDao {

    @Transaction
    @Query("SELECT * FROM exercise_log WHERE workoutId = :workoutId ORDER BY id ASC")
    fun observeLogsWithSets(workoutId: Long): Flow<List<ExerciseLogWithSets>>
}

