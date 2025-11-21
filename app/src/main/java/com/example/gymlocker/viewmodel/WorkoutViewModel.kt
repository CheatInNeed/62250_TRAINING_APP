package com.example.gymlocker.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gymlocker.data.entity.Exercises
import com.example.gymlocker.data.entity.Sets
import com.example.gymlocker.data.entity.Workout
import com.example.gymlocker.data.repository.WorkoutRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WorkoutViewModel @Inject constructor(private val workoutRepository: WorkoutRepository) : ViewModel() {

    private val _allExercises = MutableStateFlow<List<Exercises>>(emptyList())
    val allExercises: StateFlow<List<Exercises>> = _allExercises.asStateFlow()

    private val _currentWorkout = MutableStateFlow<Workout?>(null)
    val currentWorkout: StateFlow<Workout?> = _currentWorkout.asStateFlow()

    private val _workoutExercises = MutableStateFlow<List<Exercises>>(emptyList())
    val workoutExercises: StateFlow<List<Exercises>> = _workoutExercises.asStateFlow()

    init {
        viewModelScope.launch {
            workoutRepository.getAllExercises().collect {
                _allExercises.value = it
            }
        }
    }

    fun createWorkout(name: String, userId: Long) {
        viewModelScope.launch {
            val workout = Workout(name = name, date = "", userId = userId)
            val workoutId = workoutRepository.createWorkout(workout)
            _currentWorkout.value = workout.copy(workoutId = workoutId)
        }
    }

    fun addExercisesToWorkout(exercises: List<Exercises>) {
        _workoutExercises.value = _workoutExercises.value + exercises
        viewModelScope.launch {
            val workoutId = _currentWorkout.value?.workoutId ?: return@launch
            exercises.forEach { exercise ->
                workoutRepository.addExerciseToWorkout(workoutId, exercise.exerciseId)
            }
        }
    }

    fun removeExerciseFromWorkout(exercise: Exercises) {
        _workoutExercises.value = _workoutExercises.value - exercise
        viewModelScope.launch {
            val workoutId = _currentWorkout.value?.workoutId ?: return@launch
            workoutRepository.removeExerciseFromWorkout(workoutId, exercise.exerciseId)
        }
    }

    fun addSet(workoutId: Long, exerciseId: Long) {
        viewModelScope.launch {
            val currentSets = workoutRepository.getSetsForExercise(workoutId, exerciseId).first()
            val newSetNumber = currentSets.size + 1
            val newSet = Sets(
                setNumber = newSetNumber, 
                previous = "-", 
                kg = 0, 
                reps = 0, 
                isCompleted = false, 
                exerciseId = exerciseId, 
                workoutId = workoutId
            )
            workoutRepository.addSetToExercise(newSet)
        }
    }

    fun updateSet(set: Sets) {
        viewModelScope.launch {
            workoutRepository.updateSet(set)
        }
    }

    fun getSetsForExercise(workoutId: Long, exerciseId: Long): Flow<List<Sets>> {
        return workoutRepository.getSetsForExercise(workoutId, exerciseId)
    }
}
