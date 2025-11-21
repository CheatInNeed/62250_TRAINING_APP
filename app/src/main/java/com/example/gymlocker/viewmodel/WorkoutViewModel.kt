package com.example.gymlocker.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gymlocker.data.entity.Exercises
import com.example.gymlocker.data.entity.Sets
import com.example.gymlocker.data.entity.Workout
import com.example.gymlocker.data.repository.WorkoutRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

    private val _sets = MutableStateFlow<Map<Long, List<Sets>>>(emptyMap())
    val sets: StateFlow<Map<Long, List<Sets>>> = _sets.asStateFlow()

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

    fun addSet(exerciseId: Long) {
        viewModelScope.launch {
            val newSet = Sets(setNumber = (_sets.value[exerciseId]?.size ?: 0) + 1, previous = "-", kg = 0, reps = 0, isCompleted = false, exerciseId = exerciseId)
            workoutRepository.addSetToExercise(newSet)
            val newSets = _sets.value.toMutableMap()
            newSets[exerciseId] = newSets.getOrDefault(exerciseId, emptyList()) + newSet
            _sets.value = newSets
        }
    }

    fun updateSet(set: Sets) {
        viewModelScope.launch {
            workoutRepository.updateSet(set)
            val newSets = _sets.value.toMutableMap()
            val setList = newSets[set.exerciseId]?.toMutableList() ?: return@launch
            val index = setList.indexOfFirst { it.setId == set.setId }
            if (index != -1) {
                setList[index] = set
                newSets[set.exerciseId] = setList
                _sets.value = newSets
            }
        }
    }
}
