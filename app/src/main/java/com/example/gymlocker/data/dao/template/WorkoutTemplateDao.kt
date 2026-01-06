package com.example.gymlocker.data.dao.template

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.example.gymlocker.data.entity.template.*
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutTemplateDao {

    @Insert
    suspend fun insert(template: WorkoutTemplate): Long

    @Query("DELETE FROM workout_templates WHERE templateId = :templateId")
    suspend fun deleteById(templateId: Long)

    @Query("SELECT * FROM workout_templates WHERE userId = :userId ORDER BY templateId DESC")
    fun observeTemplates(userId: Long): Flow<List<WorkoutTemplate>>

    @Query("SELECT * FROM workout_templates WHERE templateId = :templateId")
    suspend fun getById(templateId: Long): WorkoutTemplate?

    @Transaction
    @Query("SELECT * FROM workout_templates WHERE templateId = :templateId")
    suspend fun getTemplateWithExercises(templateId: Long): WorkoutTemplateWithExercises?
}
