// FILE: app/src/main/java/com/example/gymlocker/data/dao/template/TemplateExerciseDao.kt
package com.example.gymlocker.data.dao.template

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.gymlocker.data.entity.template.TemplateExercise
import kotlinx.coroutines.flow.Flow

@Dao
interface TemplateExerciseDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(row: TemplateExercise): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(rows: List<TemplateExercise>): List<Long>

    @Query("SELECT * FROM template_exercise WHERE templateId = :templateId ORDER BY id ASC")
    fun observeByTemplate(templateId: Long): Flow<List<TemplateExercise>>

    @Query("SELECT * FROM template_exercise WHERE templateId = :templateId ORDER BY id ASC")
    suspend fun getByTemplateOnce(templateId: Long): List<TemplateExercise>

    // ✅ NEW: delete one template exercise (TemplateSet rows should cascade via FK)
    @Query("DELETE FROM template_exercise WHERE id = :templateExerciseId")
    suspend fun deleteById(templateExerciseId: Long): Int
}
