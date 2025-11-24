package com.example.gymlocker.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.gymlocker.data.dao.*
import com.example.gymlocker.ui.workout.NewWorkoutViewModel

class ViewModelFactory(
    private val userDao: UserDao,
    private val workoutDao: WorkoutDao,
    private val exerciseDao: ExerciseDao,
    private val exerciseLogDao: ExerciseLogDao,
    private val userWorkoutCrossRefDao: UserWorkoutCrossRefDao,
    private val muscleGroupDao: MuscleGroupDao
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(NewWorkoutViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return NewWorkoutViewModel(userDao, workoutDao, exerciseDao, exerciseLogDao, userWorkoutCrossRefDao, muscleGroupDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
