package com.example.gymlocker.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.gymlocker.data.entity.ExerciseLog
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseLogDao {
    @Insert
    suspend fun insert(exerciseLog: ExerciseLog)

    @Query("SELECT * FROM exercise_logs WHERE exerciseId = :exerciseId")
    fun getLogsForExercise(exerciseId: Long): Flow<List<ExerciseLog>>

    @Query("SELECT * FROM exercise_logs WHERE exerciseId = :exerciseId ORDER BY date DESC LIMIT 1")
    fun getLatestLogForExercise(exerciseId: Long): Flow<ExerciseLog?>
}
