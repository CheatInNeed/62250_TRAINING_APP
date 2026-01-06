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

    // --- Timer/state ---

    private val _elapsedTime = MutableStateFlow(0L)
    val elapsedTime: StateFlow<Long> = _elapsedTime.asStateFlow()

    private val _isWorkoutInProgress = MutableStateFlow(false)
    val isWorkoutInProgress: StateFlow<Boolean> = _isWorkoutInProgress.asStateFlow()

    private var timerJob: Job? = null

    // --- Workout state ---

    private val _activeExercises = MutableStateFlow<List<ActiveExerciseState>>(emptyList())
    val activeExercises: StateFlow<List<ActiveExerciseState>> = _activeExercises.asStateFlow()

    private var currentSessionId: Long? = null

    private val db by lazy { AppDatabase.getDatabase(appContext) }
    private val exerciseLogDao by lazy { db.exerciseLogDao() }

    fun completedWorkouts() = exerciseLogDao.getWorkoutSummaries()

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

    fun finishWorkout() {
        val sessionId = currentSessionId ?: System.currentTimeMillis().also { currentSessionId = it }

        viewModelScope.launch {
            // Gem alle indtastede sets (også selvom user ikke trykkede "done")
            val dateString = SimpleDateFormat(
                "yyyy-MM-dd HH:mm:ss",
                Locale.getDefault()
            ).format(Date())

            val snapshot = _activeExercises.value

            snapshot.forEach { ex ->
                ex.sets.forEach { set ->
                    // Kun gem meningsfulde rækker
                    if (set.reps > 0 && set.weight > 0) {
                        val existing = exerciseLogDao.getLogForSet(ex.exerciseId, sessionId, set.setNumber)

                        if (existing == null) {
                            exerciseLogDao.insert(
                                ExerciseLog(
                                    exerciseId = ex.exerciseId,
                                    sessionId = sessionId,
                                    setNumber = set.setNumber,
                                    reps = set.reps,
                                    weight = set.weight,
                                    date = dateString
                                )
                            )
                        } else {
                            exerciseLogDao.update(
                                existing.copy(
                                    reps = set.reps,
                                    weight = set.weight,
                                    date = dateString
                                )
                            )
                        }
                    }
                }
            }

            // Luk workout (det her er grunden til at "Resume Workout" baren forsvinder)
            stopTimer()
            _elapsedTime.value = 0
            _isWorkoutInProgress.value = false
            _activeExercises.value = emptyList()
            currentSessionId = null
        }
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
        if (existing.any { it.exerciseId == exercise.exerciseId }) return

        _activeExercises.value = existing + ActiveExerciseState(
            exerciseId = exercise.exerciseId,
            exerciseName = exercise.name
        )

        refreshPreviousForExercise(exercise.exerciseId)
    }

    fun removeExercise(exerciseId: Long) {
        val sessionId = currentSessionId ?: return

        viewModelScope.launch {
            // Slet logs (kun for den aktive workout/session)
            exerciseLogDao.deleteLogsForExerciseInSession(exerciseId, sessionId)

            // Fjern øvelsen fra UI-state
            _activeExercises.value = _activeExercises.value.filterNot { it.exerciseId == exerciseId }
        }
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
                ex.copy(sets = ex.sets.map {
                    if (it.setNumber == setNumber) it.copy(weight = newWeight) else it
                })
            } else ex
        }
    }

    fun updateSetReps(exerciseId: Long, setNumber: Int, newReps: Int) {
        _activeExercises.value = _activeExercises.value.map { ex ->
            if (ex.exerciseId == exerciseId) {
                ex.copy(sets = ex.sets.map {
                    if (it.setNumber == setNumber) it.copy(reps = newReps) else it
                })
            } else ex
        }
    }

    fun toggleSetDone(exerciseId: Long, setNumber: Int, isDone: Boolean) {
        val sessionId = currentSessionId ?: System.currentTimeMillis().also { currentSessionId = it }

        val exercise = _activeExercises.value.firstOrNull { it.exerciseId == exerciseId } ?: return
        val setBefore = exercise.sets.firstOrNull { it.setNumber == setNumber } ?: return

        _activeExercises.value = _activeExercises.value.map { ex ->
            if (ex.exerciseId == exerciseId) {
                ex.copy(sets = ex.sets.map {
                    if (it.setNumber == setNumber) it.copy(isDone = isDone) else it
                })
            } else ex
        }

        viewModelScope.launch {
            if (isDone) {

                if (setBefore.reps <= 0 || setBefore.weight <= 0) {
                    return@launch
                }

                val dateString = SimpleDateFormat(
                    "yyyy-MM-dd HH:mm:ss",
                    Locale.getDefault()
                ).format(Date())

                val existing = exerciseLogDao.getLogForSet(exerciseId, sessionId, setNumber)

                if (existing == null) {
                    exerciseLogDao.insert(
                        ExerciseLog(
                            exerciseId = exerciseId,
                            sessionId = sessionId,
                            setNumber = setNumber,
                            reps = setBefore.reps,
                            weight = setBefore.weight,
                            date = dateString
                        )
                    )
                } else {
                    exerciseLogDao.update(
                        existing.copy(
                            reps = setBefore.reps,
                            weight = setBefore.weight,
                            date = dateString
                        )
                    )
                }
            } else {
                val existing = exerciseLogDao.getLogForSet(exerciseId, sessionId, setNumber)
                if (existing != null) {
                    exerciseLogDao.delete(existing)
                }
            }

            refreshPreviousForExercise(exerciseId)
        }
    }

    // 🔑 DEN MANGLENDE FUNKTION
    private fun refreshPreviousForExercise(exerciseId: Long) {
        viewModelScope.launch {
            val logs = exerciseLogDao.getLogsForExerciseOrdered(exerciseId)
            if (logs.isEmpty()) return@launch

            // VIGTIGT: "previous" skal være sidste session FØR den nuværende workout-session
            val excludeSessionId = currentSessionId

            // logs er sorteret: sessionId DESC, setNumber ASC
            val sessionIdsInOrder = logs.map { it.sessionId }.distinct()

            val previousSessionId = sessionIdsInOrder.firstOrNull { it != excludeSessionId }
                ?: return@launch

            val previousSets = logs
                .filter { it.sessionId == previousSessionId }
                .associateBy(
                    { it.setNumber },
                    { "${it.weight}x${it.reps}" }
                )

            _activeExercises.value = _activeExercises.value.map { ex ->
                if (ex.exerciseId == exerciseId) {
                    ex.copy(
                        sets = ex.sets.map { set ->
                            set.copy(previous = previousSets[set.setNumber])
                        }
                    )
                } else ex
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
