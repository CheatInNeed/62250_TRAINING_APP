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

    // Bruges af ActiveWorkoutViewModel til at finde "previous"
    @Query(
        """
        SELECT * FROM exercise_logs 
        WHERE exerciseId = :exerciseId 
        ORDER BY sessionId DESC, setNumber ASC
        """
    )
    suspend fun getLogsForExerciseOrdered(exerciseId: Long): List<ExerciseLog>

    @Query("DELETE FROM exercise_logs WHERE exerciseId = :exerciseId AND setNumber = :setNumber")
    suspend fun deleteLogsForSet(exerciseId: Long, setNumber: Int)

    @Query("DELETE FROM exercise_logs WHERE exerciseId = :exerciseId")
    suspend fun deleteLogsForExercise(exerciseId: Long)
}
