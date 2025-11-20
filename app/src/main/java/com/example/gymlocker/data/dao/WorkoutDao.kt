package com.example.gymlocker.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.example.gymlocker.data.entity.Workout
import com.example.gymlocker.data.entity.WorkoutWithExercises
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutDao {
    @Insert
    suspend fun insert(workout: Workout): Long

    @Transaction
    @Query("SELECT * FROM workouts WHERE userId = :userId")
    fun getWorkoutsWithExercises(userId: Long): Flow<List<WorkoutWithExercises>>
}
