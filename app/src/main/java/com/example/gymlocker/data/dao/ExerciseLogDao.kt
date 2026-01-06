package com.example.gymlocker.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.gymlocker.data.entity.ExerciseLog
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseLogDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(log: ExerciseLog): Long

    @Query(
        """
        SELECT id FROM exercise_log 
        WHERE workoutId = :workoutId AND exerciseId = :exerciseId
        LIMIT 1
        """
    )
    suspend fun getLogId(workoutId: Long, exerciseId: Long): Long?

    /**
     * Ensures the (workout, exercise) log exists and returns its id.
     */
    suspend fun getOrCreateLogId(workoutId: Long, exerciseId: Long): Long {
        val existing = getLogId(workoutId, exerciseId)
        if (existing != null) return existing

        val insertedId = insert(ExerciseLog(workoutId = workoutId, exerciseId = exerciseId))
        // If another coroutine inserted it first, IGNORE returns -1
        return if (insertedId > 0) insertedId else getLogId(workoutId, exerciseId)
            ?: error("ExerciseLog insert raced but row not found afterwards.")
    }

    @Query("SELECT * FROM exercise_log WHERE workoutId = :workoutId ORDER BY id ASC")
    fun observeLogsForWorkout(workoutId: Long): Flow<List<ExerciseLog>>

    @Query("DELETE FROM exercise_log WHERE workoutId = :workoutId")
    suspend fun deleteLogsForWorkout(workoutId: Long)

    @Query("DELETE FROM exercise_log WHERE id = :logId")
    suspend fun deleteById(logId: Long)

}
