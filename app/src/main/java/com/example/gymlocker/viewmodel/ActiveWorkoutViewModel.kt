package com.example.gymlocker.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.gymlocker.data.database.AppDatabase
import com.example.gymlocker.data.entity.Exercises
import com.example.gymlocker.data.entity.PerformedSet
import com.example.gymlocker.data.entity.Workout
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

// Ét sæt (1 række i tabellen)
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
    val muscleGroupId: Long,
    val sets: List<ExerciseSetState> = listOf(ExerciseSetState(setNumber = 1))
)

class ActiveWorkoutViewModel(private val appContext: Context) : ViewModel() {

    private val db by lazy { AppDatabase.getDatabase(appContext) }
    private val workoutDao by lazy { db.workoutDao() }
    private val exerciseLogDao by lazy { db.exerciseLogDao() }
    private val performedSetDao by lazy { db.performedSetDao() }

    private var timerJob: Job? = null

    private var currentWorkoutId: Long? = null

    private val workoutCreateMutex = Mutex()

    private val _elapsedTime = MutableStateFlow(0L)
    val elapsedTime: StateFlow<Long> = _elapsedTime.asStateFlow()

    private val _isWorkoutInProgress = MutableStateFlow(false)
    val isWorkoutInProgress: StateFlow<Boolean> = _isWorkoutInProgress.asStateFlow()

    private val _activeExercises = MutableStateFlow<List<ActiveExerciseState>>(emptyList())
    val activeExercises: StateFlow<List<ActiveExerciseState>> = _activeExercises.asStateFlow()

    /**
     * Used by HomeScreen:
     * val completedWorkouts by activeWorkoutViewModel.completedWorkouts().collectAsState(...)
     */
    fun completedWorkouts() = workoutDao.getWorkoutSummaries()

    // --- Timer/state ---

    fun startTimer() {
        if (timerJob?.isActive == true) return
        _isWorkoutInProgress.value = true
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                _elapsedTime.value = _elapsedTime.value + 1
            }
        }
    }

    fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
    }

    fun discardWorkout() {
        viewModelScope.launch {
            // If we already created a Workout row, delete it (CASCADE will remove logs/sets)
            currentWorkoutId?.let { workoutDao.deleteById(it) }
            resetLocalState()
        }
    }

    private fun resetLocalState() {
        stopTimer()
        _elapsedTime.value = 0
        _isWorkoutInProgress.value = false
        _activeExercises.value = emptyList()
        currentWorkoutId = null
    }

    fun formatTime(seconds: Long): String {
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        return "$minutes min $remainingSeconds sec"
    }

    // --- Core: Option A persistence helpers ---

    private suspend fun ensureWorkoutExists(): Long = workoutCreateMutex.withLock {
        val existing = currentWorkoutId
        if (existing != null) return existing

        val userId = 1L
        val dateString = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault()).format(Date())
        val name = "Workout $dateString"

        val workoutId = workoutDao.insert(
            Workout(
                date = dateString,
                name = name,
                userId = userId
            )
        )

        currentWorkoutId = workoutId
        workoutId
    }

    private fun meaningfulSets(ex: ActiveExerciseState): List<ExerciseSetState> {
        return ex.sets.filter { it.reps > 0 && it.weight > 0 }
    }

    // --- Exercise handling ---

    fun addExercise(exercise: Exercises) {
        val existing = _activeExercises.value
        if (existing.any { it.exerciseId == exercise.exerciseId }) return

        _activeExercises.value = existing + ActiveExerciseState(
            exerciseId = exercise.exerciseId,
            exerciseName = exercise.name,
            muscleGroupId = exercise.muscleGroupId
        )

        viewModelScope.launch {
            val latest = performedSetDao.getLatestSetForExerciseAndNumberExcludingWorkout(
                exercise.exerciseId,
                1,
                currentWorkoutId
            )
            val prevText = latest?.let { formatPrevious(it.weight, it.reps) }
            setPreviousForOneSet(exercise.exerciseId, 1, prevText)
        }

        // Create the Workout + ExerciseLog lazily (so it’s ready for set saving)
        viewModelScope.launch {
            val workoutId = ensureWorkoutExists()
            exerciseLogDao.getOrCreateLogId(workoutId, exercise.exerciseId)
        }
    }

    fun removeExercise(exerciseId: Long) {
        _activeExercises.value = _activeExercises.value.filterNot { it.exerciseId == exerciseId }

        viewModelScope.launch {
            val workoutId = currentWorkoutId ?: return@launch
            val logId = exerciseLogDao.getLogId(workoutId, exerciseId) ?: return@launch
            performedSetDao.deleteSetsForLog(logId)
            exerciseLogDao.deleteById(logId)
        }
    }

    fun addSet(exerciseId: Long) {
        val current = _activeExercises.value
        val updated = current.map { ex ->
            if (ex.exerciseId != exerciseId) ex
            else {
                val nextNumber = (ex.sets.maxOfOrNull { it.setNumber } ?: 0) + 1
                ex.copy(sets = ex.sets + ExerciseSetState(setNumber = nextNumber))
            }
        }
        _activeExercises.value = updated

        val newSetNumber = updated
            .first { it.exerciseId == exerciseId }
            .sets
            .maxOf { it.setNumber }

        viewModelScope.launch {
            val latest = performedSetDao.getLatestSetForExerciseAndNumberExcludingWorkout(
                exerciseId,
                newSetNumber,
                currentWorkoutId
            )
            val prevText = latest?.let { formatPrevious(it.weight, it.reps) }
            setPreviousForOneSet(exerciseId, newSetNumber, prevText)
        }
    }

    fun removeSet(exerciseId: Long, setNumber: Int) {
        // 1) Fjern lokalt i UI-state
        _activeExercises.value = _activeExercises.value.map { ex ->
            if (ex.exerciseId != exerciseId) ex
            else {
                val newSets = ex.sets
                    .filterNot { it.setNumber == setNumber }
                    // renummerér kun i UI, så I stadig har 1..N efter sletning
                    .mapIndexed { index, s -> s.copy(setNumber = index + 1) }

                // hvis ingen sets tilbage, så behold én tom set-række
                ex.copy(sets = if (newSets.isEmpty()) listOf(ExerciseSetState(setNumber = 1)) else newSets)
            }
        }

        // 2) Slet i DB (hvis workout+log allerede findes)
        viewModelScope.launch {
            val workoutId = currentWorkoutId ?: return@launch
            val logId = exerciseLogDao.getLogId(workoutId, exerciseId) ?: return@launch
            performedSetDao.deleteSetByNumber(logId, setNumber)
        }
    }

    fun updateSetWeight(exerciseId: Long, setNumber: Int, weight: String) {
        val w = weight.toIntOrNull() ?: 0
        _activeExercises.value = _activeExercises.value.map { ex ->
            if (ex.exerciseId != exerciseId) ex
            else ex.copy(
                sets = ex.sets.map { s ->
                    if (s.setNumber == setNumber) s.copy(weight = w) else s
                }
            )
        }
    }

    fun updateSetReps(exerciseId: Long, setNumber: Int, reps: String) {
        val r = reps.toIntOrNull() ?: 0
        _activeExercises.value = _activeExercises.value.map { ex ->
            if (ex.exerciseId != exerciseId) ex
            else ex.copy(
                sets = ex.sets.map { s ->
                    if (s.setNumber == setNumber) s.copy(reps = r) else s
                }
            )
        }
    }

    fun toggleSetDone(exerciseId: Long, setNumber: Int, isDone: Boolean) {
        val before = _activeExercises.value.firstOrNull { it.exerciseId == exerciseId } ?: return
        val setBefore = before.sets.firstOrNull { it.setNumber == setNumber } ?: return

        _activeExercises.value = _activeExercises.value.map { ex ->
            if (ex.exerciseId != exerciseId) ex
            else ex.copy(
                sets = ex.sets.map { s ->
                    if (s.setNumber == setNumber) s.copy(isDone = isDone) else s
                }
            )
        }

        // Persist immediately (if meaningful)
        viewModelScope.launch {
            val workoutId = ensureWorkoutExists()
            val logId = exerciseLogDao.getOrCreateLogId(workoutId, exerciseId)

            // Only save meaningful sets (weight+reps)
            if (setBefore.reps > 0 && setBefore.weight > 0) {
                performedSetDao.upsertByNumber(
                    exerciseLogId = logId,
                    setNumber = setNumber,
                    weight = setBefore.weight.toFloat(),
                    reps = setBefore.reps,
                    isCompleted = isDone
                )
            }
        }
    }
    private suspend fun makeUniqueWorkoutNameForUser(userId: Long, baseNameRaw: String): String {
        val baseName = baseNameRaw.trim()

        // If user entered nothing meaningful, fallback to whatever name the workout already has.
        // (We'll just not update in that case, handled by caller)
        if (baseName.isBlank()) return baseName

        // Room "LIKE" pattern: "Base (" then anything after
        val likePattern = "$baseName (%"

        val existingNames = workoutDao.getNamesForAutoSuffix(
            userId = userId,
            baseName = baseName,
            likePattern = likePattern
        )

        // If baseName itself is not taken, we can use it as-is.
        if (existingNames.none { it == baseName }) return baseName

        // Parse suffix numbers: "Base (2)", "Base (3)" ...
        val regex = Regex("^${Regex.escape(baseName)} \\((\\d+)\\)$")

        val used = mutableSetOf<Int>()
        used.add(1) // baseName itself is taken -> treat as suffix 1 used

        existingNames.forEach { n ->
            val m = regex.matchEntire(n)
            val num = m?.groupValues?.getOrNull(1)?.toIntOrNull()
            if (num != null) used.add(num)
        }

        // Pick the smallest available suffix starting from 2
        var candidate = 2
        while (used.contains(candidate)) candidate++

        return "$baseName ($candidate)"
    }

    fun finishWorkout(workoutNameInput: String?) {
        viewModelScope.launch {
            val workoutId = currentWorkoutId

            // If user never added any exercises/sets, just reset without creating junk
            if (workoutId == null) {
                resetLocalState()
                return@launch
            }

            val userId = 1L // matches your current approach in ensureWorkoutExists()

            // ✅ 1) Store the name (with auto-suffix) BEFORE resetting state
            val typed = workoutNameInput?.trim().orEmpty()
            if (typed.isNotBlank()) {
                val uniqueName = makeUniqueWorkoutNameForUser(userId, typed)
                workoutDao.updateWorkoutName(workoutId, uniqueName)
            }

            val snapshot = _activeExercises.value

            // Persist snapshot into (exercise_log + performed_set)
            snapshot.forEach { ex ->
                val sets = meaningfulSets(ex)

                // If no meaningful sets for that exercise, remove the log (don’t count it as completed)
                if (sets.isEmpty()) {
                    exerciseLogDao.getLogId(workoutId, ex.exerciseId)?.let { logId ->
                        performedSetDao.deleteSetsForLog(logId)
                        exerciseLogDao.deleteById(logId)
                    }
                    return@forEach
                }

                val logId = exerciseLogDao.getOrCreateLogId(workoutId, ex.exerciseId)

                // Replace sets for this log with the current snapshot
                performedSetDao.deleteSetsForLog(logId)
                sets.forEach { s ->
                    performedSetDao.insert(
                        PerformedSet(
                            exerciseLogId = logId,
                            setNumber = s.setNumber,
                            weight = s.weight.toFloat(),
                            reps = s.reps,
                            isCompleted = s.isDone
                        )
                    )
                }
            }

            // Reset local state after saving
            resetLocalState()
        }
    }


    // --- Factory ---

    companion object {
        fun provideFactory(context: Context): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return ActiveWorkoutViewModel(context.applicationContext) as T
                }
            }
        }
    }

    private fun setPreviousForExercise(exerciseId: Long, previousText: String?) {
        _activeExercises.value = _activeExercises.value.map { ex ->
            if (ex.exerciseId != exerciseId) ex
            else ex.copy(
                sets = ex.sets.map { s ->
                    // Sæt "previous" for alle sets (eller kun set 1 hvis du vil)
                    s.copy(previous = previousText)
                }
            )
        }
    }

    private fun formatPrevious(weight: Float, reps: Int): String =
        "${weight.toInt()} kg x $reps"

    private fun setPreviousForOneSet(exerciseId: Long, setNumber: Int, previousText: String?) {
        _activeExercises.value = _activeExercises.value.map { ex ->
            if (ex.exerciseId != exerciseId) ex
            else ex.copy(
                sets = ex.sets.map { s ->
                    if (s.setNumber == setNumber) s.copy(previous = previousText) else s
                }
            )
        }
    }

    suspend fun getMuscleGroupName(id: Long): String {
        return db.muscleGroupDao().getNameById(id) ?: "Unknown"
    }
}
