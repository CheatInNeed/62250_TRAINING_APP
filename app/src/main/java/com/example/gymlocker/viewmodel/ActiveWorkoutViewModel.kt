package com.example.gymlocker.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.gymlocker.data.database.AppDatabase
import com.example.gymlocker.data.entity.ExerciseLog
import com.example.gymlocker.data.entity.Exercises
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Ét sæt (1 række i din tabel)
data class ExerciseSetState(
    val setNumber: Int,
    val weight: Int = 0,
    val reps: Int = 0,
    val isDone: Boolean = false,
    val previous: String? = null
)

// Én øvelse i den aktive workout
data class ActiveExerciseState(
    val exerciseId: Long,
    val exerciseName: String,
    val sets: List<ExerciseSetState> = listOf(ExerciseSetState(setNumber = 1))
)

class ActiveWorkoutViewModel(private val appContext: Context) : ViewModel() {

    // --- Timer/state fra din oprindelige VM ---

    private val _elapsedTime = MutableStateFlow(0L)
    val elapsedTime: StateFlow<Long> = _elapsedTime.asStateFlow()

    private val _isWorkoutInProgress = MutableStateFlow(false)
    val isWorkoutInProgress: StateFlow<Boolean> = _isWorkoutInProgress.asStateFlow()

    private var timerJob: Job? = null

    // --- Ny state: aktive øvelser + session ---

    private val _activeExercises = MutableStateFlow<List<ActiveExerciseState>>(emptyList())
    val activeExercises: StateFlow<List<ActiveExerciseState>> = _activeExercises.asStateFlow()

    private var currentSessionId: Long? = null

    private val db by lazy { AppDatabase.getDatabase(appContext) }
    private val exerciseLogDao by lazy { db.exerciseLogDao() }

    fun startTimer() {
        if (timerJob?.isActive == true) return
        if (currentSessionId == null) {
            currentSessionId = System.currentTimeMillis()
        }
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
        _activeExercises.value = emptyList()
        currentSessionId = null
    }

    fun formatTime(seconds: Long): String {
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        return "$minutes min $remainingSeconds sec"
    }

    // --- Øvelses-håndtering ---

    fun addExercise(exercise: Exercises) {
        if (currentSessionId == null) {
            currentSessionId = System.currentTimeMillis()
        }
        val existing = _activeExercises.value
        if (existing.any { it.exerciseId == exercise.exerciseId }) {
            return // allerede tilføjet
        }
        val newList = existing + ActiveExerciseState(
            exerciseId = exercise.exerciseId,
            exerciseName = exercise.name
        )
        _activeExercises.value = newList

        refreshPreviousForExercise(exercise.exerciseId)
    }

    fun addSet(exerciseId: Long) {
        _activeExercises.value = _activeExercises.value.map { ex ->
            if (ex.exerciseId == exerciseId) {
                val nextNumber = (ex.sets.maxOfOrNull { it.setNumber } ?: 0) + 1
                ex.copy(sets = ex.sets + ExerciseSetState(setNumber = nextNumber))
            } else ex
        }
        refreshPreviousForExercise(exerciseId)
    }

    fun updateSetWeight(exerciseId: Long, setNumber: Int, newWeight: Int) {
        _activeExercises.value = _activeExercises.value.map { ex ->
            if (ex.exerciseId == exerciseId) {
                ex.copy(sets = ex.sets.map { set ->
                    if (set.setNumber == setNumber) set.copy(weight = newWeight) else set
                })
            } else ex
        }
    }

    fun updateSetReps(exerciseId: Long, setNumber: Int, newReps: Int) {
        _activeExercises.value = _activeExercises.value.map { ex ->
            if (ex.exerciseId == exerciseId) {
                ex.copy(sets = ex.sets.map { set ->
                    if (set.setNumber == setNumber) set.copy(reps = newReps) else set
                })
            } else ex
        }
    }

    fun toggleSetDone(exerciseId: Long, setNumber: Int, isDone: Boolean) {
        val sessionId = currentSessionId ?: System.currentTimeMillis().also { currentSessionId = it }

        val exercise = _activeExercises.value.firstOrNull { it.exerciseId == exerciseId } ?: return
        val set = exercise.sets.firstOrNull { it.setNumber == setNumber } ?: return

        _activeExercises.value = _activeExercises.value.map { ex ->
            if (ex.exerciseId == exerciseId) {
                ex.copy(sets = ex.sets.map { s ->
                    if (s.setNumber == setNumber) s.copy(isDone = isDone) else s
                })
            } else ex
        }

        if (isDone) {
            viewModelScope.launch {
                val now = System.currentTimeMillis()
                val dateString = SimpleDateFormat(
                    "yyyy-MM-dd HH:mm:ss",
                    Locale.getDefault()
                ).format(Date(now))

                val log = ExerciseLog(
                    exerciseId = exerciseId,
                    sessionId = sessionId,
                    setNumber = setNumber,
                    reps = set.reps,
                    weight = set.weight,
                    date = dateString
                )
                exerciseLogDao.insert(log)

                // Opdater "previous" efter vi har logget
                refreshPreviousForExercise(exerciseId)
            }
        }
    }

    fun deleteSet(exerciseId: Long, setNumber: Int) {
        _activeExercises.value = _activeExercises.value.map { ex ->
            if (ex.exerciseId == exerciseId) {
                val filtered = ex.sets
                    .filterNot { it.setNumber == setNumber }
                    .sortedBy { it.setNumber }
                val renumbered = filtered.mapIndexed { index, set ->
                    set.copy(setNumber = index + 1)
                }
                ex.copy(sets = renumbered)
            } else ex
        }

        viewModelScope.launch {
            exerciseLogDao.deleteLogsForSet(exerciseId, setNumber)
        }

        refreshPreviousForExercise(exerciseId)
    }

    fun deleteExercise(exerciseId: Long) {
        _activeExercises.value = _activeExercises.value.filterNot {
            it.exerciseId == exerciseId
        }

        viewModelScope.launch {
            exerciseLogDao.deleteLogsForExercise(exerciseId)
        }
    }

    private fun refreshPreviousForExercise(exerciseId: Long) {
        val thisSessionId = currentSessionId
        viewModelScope.launch {
            val logs = exerciseLogDao.getLogsForExerciseOrdered(exerciseId)
            val previousSessionId = logs.firstOrNull { it.sessionId != thisSessionId }?.sessionId

            if (previousSessionId == null) {
                // Ingen tidligere workouts for denne øvelse
                _activeExercises.value = _activeExercises.value.map { ex ->
                    if (ex.exerciseId == exerciseId) {
                        ex.copy(sets = ex.sets.map { it.copy(previous = null) })
                    } else ex
                }
            } else {
                val previousLogs = logs.filter { it.sessionId == previousSessionId }
                val previousMap = previousLogs.associate { log ->
                    log.setNumber to "${log.weight}x${log.reps}"
                }

                _activeExercises.value = _activeExercises.value.map { ex ->
                    if (ex.exerciseId == exerciseId) {
                        ex.copy(sets = ex.sets.map { set ->
                            set.copy(previous = previousMap[set.setNumber])
                        })
                    } else ex
                }
            }
        }
    }

    companion object {
        fun provideFactory(context: Context): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(ActiveWorkoutViewModel::class.java)) {
                        return ActiveWorkoutViewModel(context.applicationContext) as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel class")
                }
            }
    }
}
