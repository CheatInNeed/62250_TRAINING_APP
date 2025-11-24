package com.example.gymlocker.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.gymlocker.data.entity.Exercises
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(exercise: Exercises): Long

    @Query("SELECT * FROM exercises")
    fun getAllExercises(): Flow<List<Exercises>>

    @Query("SELECT * FROM exercises WHERE name = :name")
    fun getExerciseByName(name: String): Flow<Exercises?>
}
