package com.example.gymlocker.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.gymlocker.data.entity.Exercises
import kotlinx.coroutines.flow.Flow
import androidx.room.OnConflictStrategy

@Dao
interface ExerciseDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(exercise: Exercises)

    @Query("SELECT * FROM exercises")
    fun getAllExercises(): Flow<List<Exercises>>

    @Query(
        """
        SELECT * FROM exercises
        WHERE ownerUserId IS NULL
           OR ownerUserId = :userId
        ORDER BY name COLLATE NOCASE ASC
        """
    )
    fun getExercisesForUser(userId: Long): Flow<List<Exercises>>

    @Query("SELECT COUNT(*) FROM exercises")
    suspend fun countExercises(): Int

    @Query("SELECT * FROM exercises WHERE exerciseId = :id LIMIT 1")
    suspend fun getById(id: Long): Exercises?

    @Query("SELECT exerciseId FROM exercises WHERE name = :name LIMIT 1")
    suspend fun getExerciseIdByName(name: String): Long?

    // old global check: keep for seeding/import
    @Query("SELECT EXISTS(SELECT 1 FROM exercises WHERE LOWER(name) = LOWER(:name))")
    suspend fun existsByNameIgnoreCase(name: String): Boolean

    // NEW: for user-created exercises: check against global + this user
    @Query(
        """
        SELECT EXISTS(
            SELECT 1
            FROM exercises
            WHERE LOWER(name) = LOWER(:name)
              AND (ownerUserId IS NULL OR ownerUserId = :userId)
        )
        """
    )
    suspend fun existsByNameIgnoreCaseForUserOrGlobal(userId: Long, name: String): Boolean

    @Query("SELECT * FROM exercises ORDER BY exerciseId ASC")
    suspend fun getAllOnce(): List<Exercises>
}

