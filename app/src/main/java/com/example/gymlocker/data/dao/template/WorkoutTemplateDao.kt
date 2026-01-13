package com.example.gymlocker.data.dao.template

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.gymlocker.data.entity.template.*
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutTemplateDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(template: WorkoutTemplate): Long

    @Update
    suspend fun update(template: WorkoutTemplate)

    @Query("DELETE FROM workout_templates WHERE templateId = :templateId")
    suspend fun deleteById(templateId: Long)

    @Query("SELECT * FROM workout_templates WHERE userId = :userId ORDER BY templateId DESC")
    fun observeTemplates(userId: Long): Flow<List<WorkoutTemplate>>

    @Query("SELECT * FROM workout_templates WHERE templateId = :templateId")
    suspend fun getById(templateId: Long): WorkoutTemplate?

    @Transaction
    @Query("SELECT * FROM workout_templates WHERE templateId = :templateId")
    suspend fun getTemplateWithExercises(templateId: Long): WorkoutTemplateWithExercises?

    @Query("SELECT COUNT(*) FROM workout_templates WHERE userId = :userId")
    suspend fun countTemplatesByUserId(userId: Long): Int

    @Query("UPDATE workout_templates SET isFavorite = NOT isFavorite WHERE templateId = :templateId")
    suspend fun toggleFavorite(templateId: Long)
}
