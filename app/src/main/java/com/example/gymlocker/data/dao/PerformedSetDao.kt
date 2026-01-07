package com.example.gymlocker.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.gymlocker.data.entity.PerformedSet
import kotlinx.coroutines.flow.Flow

@Dao
interface PerformedSetDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(set: PerformedSet): Long

    @Update
    suspend fun update(set: PerformedSet)

    @Query(
        """
        SELECT * FROM performed_set
        WHERE exerciseLogId = :exerciseLogId
        ORDER BY setNumber ASC
        """
    )
    fun observeSetsForLog(exerciseLogId: Long): Flow<List<PerformedSet>>

    @Query(
        """
        SELECT * FROM performed_set
        WHERE exerciseLogId = :exerciseLogId AND setNumber = :setNumber
        LIMIT 1
        """
    )
    suspend fun getSetByNumber(exerciseLogId: Long, setNumber: Int): PerformedSet?

    /**
     * Upsert by (exerciseLogId, setNumber) uniqueness.
     */
    suspend fun upsertByNumber(exerciseLogId: Long, setNumber: Int, weight: Float, reps: Int, isCompleted: Boolean) {
        val existing = getSetByNumber(exerciseLogId, setNumber)
        if (existing == null) {
            insert(
                PerformedSet(
                    exerciseLogId = exerciseLogId,
                    setNumber = setNumber,
                    weight = weight,
                    reps = reps,
                    isCompleted = isCompleted
                )
            )
        } else {
            update(
                existing.copy(
                    weight = weight,
                    reps = reps,
                    isCompleted = isCompleted
                )
            )
        }
    }

    @Query("DELETE FROM performed_set WHERE exerciseLogId = :exerciseLogId")
    suspend fun deleteSetsForLog(exerciseLogId: Long)

    @Query(
        """
        DELETE FROM performed_set
        WHERE exerciseLogId IN (SELECT id FROM exercise_log WHERE workoutId = :workoutId)
        """
    )
    suspend fun deleteSetsForWorkout(workoutId: Long)

    @Query(
        """
    DELETE FROM performed_set
    WHERE exerciseLogId = :exerciseLogId
      AND setNumber = :setNumber
    """
    )
    suspend fun deleteSetByNumber(exerciseLogId: Long, setNumber: Int)

    @Query(
        """
    SELECT ps.* FROM performed_set ps
    JOIN exercise_log el ON el.id = ps.exerciseLogId
    JOIN workouts w ON w.workoutId = el.workoutId
    WHERE el.exerciseId = :exerciseId
      AND ps.setNumber = :setNumber
      AND (:excludeWorkoutId IS NULL OR w.workoutId != :excludeWorkoutId)
    ORDER BY w.date DESC
    LIMIT 1
    """
    )
    suspend fun getLatestSetForExerciseAndNumberExcludingWorkout(
        exerciseId: Long,
        setNumber: Int,
        excludeWorkoutId: Long?
    ): PerformedSet?

    @Query("SELECT * FROM performed_set WHERE exerciseLogId = :exerciseLogId ORDER BY setNumber ASC")
    suspend fun getSetsForLogOnce(exerciseLogId: Long): List<PerformedSet>

}
