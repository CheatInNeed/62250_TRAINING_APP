package com.example.gymlocker.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ActiveWorkoutViewModel : ViewModel() {

    private val _elapsedTime = MutableStateFlow(0L)
    val elapsedTime: StateFlow<Long> = _elapsedTime

    private val _isWorkoutInProgress = MutableStateFlow(false)
    val isWorkoutInProgress: StateFlow<Boolean> = _isWorkoutInProgress

    private var timerJob: Job? = null

    fun startTimer() {
        if (timerJob?.isActive == true) return
        _isWorkoutInProgress.value = true
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

    fun discardWorkout() {
        stopTimer()
        _elapsedTime.value = 0
        _isWorkoutInProgress.value = false
    }

    fun formatTime(seconds: Long): String {
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        return "$minutes min $remainingSeconds sec"
    }
}
