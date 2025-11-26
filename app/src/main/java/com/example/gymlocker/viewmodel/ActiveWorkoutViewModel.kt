package com.example.gymlocker.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gymlocker.ui.workout.WorkoutExercise
import com.example.gymlocker.ui.workout.WorkoutSet
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ActiveWorkoutViewModel : ViewModel() {
    private val _isWorkoutInProgress = MutableStateFlow(false)
    val isWorkoutInProgress = _isWorkoutInProgress.asStateFlow()

    private val _elapsedTime = MutableStateFlow(0L)
    val elapsedTime = _elapsedTime.asStateFlow()

    private var timerJob: Job? = null

    private val _exercises = MutableStateFlow<List<WorkoutExercise>>(emptyList())
    val exercises = _exercises.asStateFlow()

    fun addExercise(exerciseName: String) {
        val currentExercises = _exercises.value.toMutableList()
        if (!currentExercises.any { it.name == exerciseName }) {
            currentExercises.add(WorkoutExercise(exerciseName, emptyList()))
            _exercises.value = currentExercises
        }
    }

    fun addSetToExercise(exerciseName: String, reps: String, weight: String) {
        val currentExercises = _exercises.value.toMutableList()
        val exerciseIndex = currentExercises.indexOfFirst { it.name == exerciseName }
        if (exerciseIndex != -1) {
            val exercise = currentExercises[exerciseIndex]
            val updatedSets = exercise.sets.toMutableList().apply {
                add(WorkoutSet(reps, weight))
            }
            currentExercises[exerciseIndex] = exercise.copy(sets = updatedSets)
            _exercises.value = currentExercises
        }
    }

    fun startTimer() {
        _isWorkoutInProgress.value = true
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                _elapsedTime.value++
            }
        }
    }

    fun stopTimer() {
        timerJob?.cancel()
    }

    fun finishWorkout() {
        stopTimer()
        _isWorkoutInProgress.value = false
        _elapsedTime.value = 0
        _exercises.value = emptyList()
    }

    fun discardWorkout() {
        finishWorkout()
    }

    fun formatTime(seconds: Long): String {
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val secs = seconds % 60
        return String.format("%02d:%02d:%02d", hours, minutes, secs)
    }

    override fun onCleared() {
        super.onCleared()
        stopTimer()
    }
}