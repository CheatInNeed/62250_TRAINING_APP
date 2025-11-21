package com.example.gymlocker.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.gymlocker.data.entity.Sets
import kotlinx.coroutines.flow.Flow

@Dao
interface SetsDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(sets: Sets)

    @Update
    suspend fun update(sets: Sets)

    @Delete
    suspend fun delete(sets: Sets)

    @Query("SELECT * FROM sets WHERE exerciseId = :exerciseId")
    fun getSetsForExercise(exerciseId: Long): Flow<List<Sets>>
}
