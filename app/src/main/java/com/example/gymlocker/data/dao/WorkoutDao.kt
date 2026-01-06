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

    @Query(
        """
        SELECT 
            w.workoutId AS workoutId,
            w.date AS date,
            COUNT(el.id) AS exerciseCount
        FROM workouts w
        LEFT JOIN exercise_log el ON el.workoutId = w.workoutId
        GROUP BY w.workoutId
        ORDER BY w.workoutId DESC
        """
    )
    fun getWorkoutSummaries(): Flow<List<WorkoutSummary>>
}
