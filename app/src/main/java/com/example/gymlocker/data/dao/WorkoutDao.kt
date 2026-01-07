package com.example.gymlocker.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.gymlocker.data.entity.Workout
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutDao {

    @Insert
    suspend fun insert(workout: Workout): Long

    @Query("DELETE FROM workouts WHERE workoutId = :workoutId")
    suspend fun deleteById(workoutId: Long)

    // ✅ Update the workout's name when the user finishes
    @Query("UPDATE workouts SET name = :name WHERE workoutId = :workoutId")
    suspend fun updateWorkoutName(workoutId: Long, name: String)

    /**
     * ✅ Used for auto-suffixing:
     * We fetch all names that are either exactly baseName OR start with "baseName ("
     * for the same user.
     */
    @Query(
        """
        SELECT name FROM workouts
        WHERE userId = :userId
          AND (name = :baseName OR name LIKE :likePattern)
        """
    )
    suspend fun getNamesForAutoSuffix(
        userId: Long,
        baseName: String,
        likePattern: String
    ): List<String>

    @Query(
        """
        SELECT 
            w.workoutId AS workoutId,
            w.date AS date,
            w.name AS name,
            COUNT(el.id) AS exerciseCount
        FROM workouts w
        LEFT JOIN exercise_log el ON el.workoutId = w.workoutId
        GROUP BY w.workoutId
        ORDER BY w.workoutId DESC
        """
    )
    fun getWorkoutSummaries(): Flow<List<WorkoutSummary>>
}
