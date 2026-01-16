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

    @Query("UPDATE workouts SET name = :name WHERE workoutId = :workoutId")
    suspend fun updateWorkoutName(workoutId: Long, name: String)

    // ✅ NEW: persist duration
    @Query("UPDATE workouts SET time = :timeSeconds WHERE workoutId = :workoutId")
    suspend fun updateWorkoutTime(workoutId: Long, timeSeconds: Long)

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

    /*// Existing (global) summaries (you already have it)
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
    fun getWorkoutSummaries(): Flow<List<WorkoutSummary>>*/

    // ✅ NEW: pull workouts from a date-string boundary (works because your date format is lexicographically sortable)
    @Query(
        """
        SELECT * FROM workouts
        WHERE userId = :userId
          AND date >= :startInclusive
        ORDER BY date ASC
        """
    )
    fun observeWorkoutsFrom(userId: Long, startInclusive: String): Flow<List<Workout>>

    /*// ✅ NEW: user-filtered summaries (Profile needs this)
    @Query(
        """
        SELECT 
            w.workoutId AS workoutId,
            w.date AS date,
            w.name AS name,
            COUNT(el.id) AS exerciseCount
        FROM workouts w
        LEFT JOIN exercise_log el ON el.workoutId = w.workoutId
        WHERE w.userId = :userId
        GROUP BY w.workoutId
        ORDER BY w.workoutId DESC
        """
    )
    fun getWorkoutSummariesForUser(userId: Long): Flow<List<WorkoutSummary>>*/

    // ✅ NEW: total workouts for Profile summary
    @Query("SELECT COUNT(*) FROM workouts WHERE userId = :userId")
    suspend fun countWorkoutsForUser(userId: Long): Int

    // ✅ NEW: most recent workout (name/date)
    @Query(
        """
    SELECT 
        workoutId AS workoutId, 
        date AS date, 
        name AS name, 
        0 AS exerciseCount,
        NULL AS exerciseSetSummary
    FROM workouts
    WHERE userId = :userId
    ORDER BY workoutId DESC
    LIMIT 1
    """
    )
    suspend fun getMostRecentWorkoutForUser(userId: Long): WorkoutSummary?

    @Query("SELECT name FROM workouts WHERE workoutId = :workoutId LIMIT 1")
    suspend fun getWorkoutNameById(workoutId: Long): String?

    @Query("SELECT * FROM workouts WHERE workoutId = :workoutId LIMIT 1")
    suspend fun getWorkoutById(workoutId: Long): Workout?

    @Query(
        """
    SELECT 
        w.workoutId AS workoutId,
        w.date AS date,
        w.name AS name,
        COUNT(DISTINCT el.id) AS exerciseCount,
        GROUP_CONCAT(
            COALESCE(ps.setCount, 0) || 'x ' || e.name,
            ', '
        ) AS exerciseSetSummary
    FROM workouts w
    LEFT JOIN exercise_log el ON el.workoutId = w.workoutId
    LEFT JOIN exercises e ON e.exerciseId = el.exerciseId
    LEFT JOIN (
        SELECT 
            exerciseLogId,
            COUNT(*) AS setCount
        FROM performed_set
        WHERE isCompleted = 1
        GROUP BY exerciseLogId
    ) ps ON ps.exerciseLogId = el.id
    GROUP BY w.workoutId
    ORDER BY w.workoutId DESC
    """
    )
    fun getWorkoutSummaries(): Flow<List<WorkoutSummary>>

    @Query(
        """
    SELECT 
        w.workoutId AS workoutId,
        w.date AS date,
        w.name AS name,
        COUNT(DISTINCT el.id) AS exerciseCount,
        GROUP_CONCAT(
            COALESCE(ps.setCount, 0) || 'x ' || e.name,
            ', '
        ) AS exerciseSetSummary
    FROM workouts w
    LEFT JOIN exercise_log el ON el.workoutId = w.workoutId
    LEFT JOIN exercises e ON e.exerciseId = el.exerciseId
    LEFT JOIN (
        SELECT 
            exerciseLogId,
            COUNT(*) AS setCount
        FROM performed_set
        WHERE isCompleted = 1
        GROUP BY exerciseLogId
    ) ps ON ps.exerciseLogId = el.id
    WHERE w.userId = :userId
    GROUP BY w.workoutId
    ORDER BY w.workoutId DESC
    """
    )
    fun getWorkoutSummariesForUser(userId: Long): Flow<List<WorkoutSummary>>

    @Query("SELECT COUNT(*) FROM workouts WHERE userId = :userId AND name = :name")
    suspend fun countByName(userId: Long, name: String): Int
}
