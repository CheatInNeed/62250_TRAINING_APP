package com.example.gymlocker.data.dao

import androidx.room.*
import com.example.gymlocker.data.entity.ExerciseLog
import kotlinx.coroutines.flow.Flow


data class WorkoutSummary(
    val sessionId: Long,
    val date: String,
    val exerciseCount: Int
)

@Dao
interface ExerciseLogDao {
    @Insert
    suspend fun insert(exerciseLog: ExerciseLog)

    @Update
    suspend fun update(exerciseLog: ExerciseLog)

    @Delete
    suspend fun delete(exerciseLog: ExerciseLog)

    @Query("DELETE FROM exercise_logs WHERE logId = :logId")
    suspend fun deleteById(logId: Long)

    @Query("""
        SELECT * FROM exercise_logs 
        WHERE exerciseId = :exerciseId
    """)
    fun getLogsForExercise(exerciseId: Long): Flow<List<ExerciseLog>>

    @Query("""
    SELECT 
        sessionId AS sessionId,
        MAX(date) AS date,
        COUNT(DISTINCT exerciseId) AS exerciseCount
    FROM exercise_logs
    GROUP BY sessionId
    ORDER BY sessionId DESC
""")
    fun getWorkoutSummaries(): kotlinx.coroutines.flow.Flow<List<WorkoutSummary>>

    @Query("""
        SELECT * FROM exercise_logs 
        WHERE exerciseId = :exerciseId 
        ORDER BY sessionId DESC, setNumber ASC
    """)
    suspend fun getLogsForExerciseOrdered(exerciseId: Long): List<ExerciseLog>

    @Query("""
    DELETE FROM exercise_logs 
    WHERE exerciseId = :exerciseId AND sessionId = :sessionId
""")
    suspend fun deleteLogsForExerciseInSession(exerciseId: Long, sessionId: Long)

    // Find log for et set i en given session (så vi kan update/delete korrekt)
    @Query("""
        SELECT * FROM exercise_logs
        WHERE exerciseId = :exerciseId 
          AND sessionId = :sessionId
          AND setNumber = :setNumber
        LIMIT 1
    """)
    suspend fun getLogForSet(exerciseId: Long, sessionId: Long, setNumber: Int): ExerciseLog?
}
