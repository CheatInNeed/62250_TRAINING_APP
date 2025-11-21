package com.example.gymlocker.data.repository

import com.example.gymlocker.data.dao.ExerciseDao
import com.example.gymlocker.data.dao.SetsDao
import com.example.gymlocker.data.dao.WorkoutDao
import com.example.gymlocker.data.dao.WorkoutExerciseCrossRefDao
import com.example.gymlocker.data.entity.Exercises
import com.example.gymlocker.data.entity.Sets
import com.example.gymlocker.data.entity.Workout
import com.example.gymlocker.data.entity.WorkoutExerciseCrossRef
import kotlinx.coroutines.flow.Flow

class WorkoutRepository(
    private val workoutDao: WorkoutDao,
    private val exerciseDao: ExerciseDao,
    private val setsDao: SetsDao,
    private val workoutExerciseCrossRefDao: WorkoutExerciseCrossRefDao
) {
    fun getAllExercises(): Flow<List<Exercises>> = exerciseDao.getAllExercises()

    suspend fun createWorkout(workout: Workout): Long = workoutDao.insert(workout)

    suspend fun addExerciseToWorkout(workoutId: Long, exerciseId: Long) {
        workoutExerciseCrossRefDao.insert(WorkoutExerciseCrossRef(workoutId, exerciseId))
    }

    suspend fun removeExerciseFromWorkout(workoutId: Long, exerciseId: Long) {
        workoutExerciseCrossRefDao.delete(WorkoutExerciseCrossRef(workoutId, exerciseId))
    }

    fun getSetsForExercise(workoutId: Long, exerciseId: Long): Flow<List<Sets>> = setsDao.getSetsForExercise(workoutId, exerciseId)

    suspend fun addSetToExercise(set: Sets) {
        setsDao.insert(set)
    }

    suspend fun updateSet(set: Sets) {
        setsDao.update(set)
    }
}
