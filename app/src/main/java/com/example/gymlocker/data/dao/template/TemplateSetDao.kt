package com.example.gymlocker.data.dao.template

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.gymlocker.data.entity.template.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TemplateSetDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(set: TemplateSet): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(sets: List<TemplateSet>): List<Long>

    @Query("SELECT * FROM template_set WHERE templateExerciseId = :templateExerciseId ORDER BY setNumber ASC")
    fun observeByTemplateExercise(templateExerciseId: Long): Flow<List<TemplateSet>>

    @Query("SELECT * FROM template_set WHERE templateExerciseId = :templateExerciseId ORDER BY setNumber ASC")
    suspend fun getByTemplateExerciseOnce(templateExerciseId: Long): List<TemplateSet>
}
