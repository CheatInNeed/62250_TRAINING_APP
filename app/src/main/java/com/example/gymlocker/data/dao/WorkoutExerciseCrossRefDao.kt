package com.example.gymlocker.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import com.example.gymlocker.data.entity.WorkoutExerciseCrossRef

@Dao
interface WorkoutExerciseCrossRefDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(crossRef: WorkoutExerciseCrossRef)

    @Delete
    suspend fun delete(crossRef: WorkoutExerciseCrossRef)
}
