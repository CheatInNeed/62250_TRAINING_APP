package com.example.gymlocker.data.dao

import androidx.room.Dao
import androidx.room.Insert
import com.example.gymlocker.data.entity.WorkoutExerciseCrossRef

@Dao
interface UserWorkoutCrossRefDao {
    @Insert
    suspend fun insert(crossRef: WorkoutExerciseCrossRef)
}
