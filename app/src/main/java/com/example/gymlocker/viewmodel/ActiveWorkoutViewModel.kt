package com.example.gymlocker.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.gymlocker.data.database.AppDatabase
import com.example.gymlocker.data.entity.*
import com.example.gymlocker.data.entity.template.*
import com.example.gymlocker.data.dao.*
import com.example.gymlocker.data.dao.template.*
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
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import com.example.gymlocker.data.dao.WorkoutSummary
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.temporal.ChronoUnit

// Ét sæt (1 række i tabellen)
data class ExerciseSetState(
    val setNumber: Int,
    val weight: Int = 0,
    val reps: Int = 0,
    val isDone: Boolean = false,
    val previous: String? = null,

    // NEW: visual hint flags for opacity
    val isWeightPrefilled: Boolean = false,
    val isRepsPrefilled: Boolean = false
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
    private val exerciseDao by lazy { db.exerciseDao() }
    private val performedSetDao by lazy { db.performedSetDao() }

    private val workoutTemplateDao by lazy { db.workoutTemplateDao() }
    private val templateExerciseDao by lazy { db.templateExerciseDao() }
    private val templateSetDao by lazy { db.templateSetDao() }


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

    /**
     * Used by HomeScreen:
     * val lastWorkoutLabel by activeWorkoutViewModel.lastWorkoutLabel().collectAsState("...")
     */
    fun lastWorkoutLabel() = workoutDao.getWorkoutSummaries().map { workouts ->
        makeLastWorkoutLabel(workouts)
    }

    private fun makeLastWorkoutLabel(workouts: List<WorkoutSummary>): String {
        if (workouts.isEmpty()) return "Ingen tidligere workouts — klar til din første? 🚀"

        // getWorkoutSummaries() er ORDER BY workoutId DESC, så første er nyeste :contentReference[oaicite:2]{index=2}
        val latest = workouts.first()

        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
        val lastDate = runCatching {
            LocalDateTime.parse(latest.date, formatter).toLocalDate()
        }.getOrNull() ?: return "Sidste workout: ukendt"

        val today = LocalDate.now()
        val days = ChronoUnit.DAYS.between(lastDate, today).toInt()

        return when (days) {
            0 -> "Trænede i dag 🔥 Keep it going!"
            1 -> "Trænede i går 💪 Skal vi tage en mere?"
            else -> "Sidste workout: $days dage siden — tid til at komme afsted 🚀"
        }
    }


    /**
     * Home screen templates: observe seeded/user templates for the default user (1L).
     */
    fun observeTemplates(userId: Long = 1L) = workoutTemplateDao.observeTemplates(userId)

    /**
     * Fetch a specific template with its exercises for the detail screen.
     */
    suspend fun getTemplateWithExercises(templateId: Long) =
        workoutTemplateDao.getTemplateWithExercises(templateId)

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

        // Add placeholder exercise first (so UI shows it immediately)
        _activeExercises.value = existing + ActiveExerciseState(
            exerciseId = exercise.exerciseId,
            exerciseName = exercise.name,
            muscleGroupId = exercise.muscleGroupId,
            sets = listOf(ExerciseSetState(setNumber = 1))
        )

        viewModelScope.launch {
            // Find latest workout containing this exercise (excluding current in-progress workout)
            val latestWorkoutId = performedSetDao.getLatestWorkoutIdForExerciseExcludingWorkout(
                exerciseId = exercise.exerciseId,
                excludeWorkoutId = currentWorkoutId
            )

            if (latestWorkoutId == null) {
                // No previous data -> keep default single empty set
                // previous will be shown as "-" by UI already
                return@launch
            }

            val previousSets = performedSetDao.getPerformedSetsForExerciseInWorkout(
                workoutId = latestWorkoutId,
                exerciseId = exercise.exerciseId
            )

            if (previousSets.isEmpty()) return@launch

            // Convert previous performed sets to UI sets
            val clonedSets = previousSets.map { ps ->
                val w = ps.weight.toInt()
                val r = ps.reps
                ExerciseSetState(
                    setNumber = ps.setNumber,
                    weight = w,
                    reps = r,
                    isDone = false, // never auto-complete
                    previous = formatPrevious(ps.weight, ps.reps),
                    isWeightPrefilled = true,
                    isRepsPrefilled = true
                )
            }

            // Apply cloned sets to the exercise in UI state
            _activeExercises.value = _activeExercises.value.map { ex ->
                if (ex.exerciseId != exercise.exerciseId) ex
                else ex.copy(sets = clonedSets)
            }
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
                exerciseId = exerciseId,
                setNumber = newSetNumber,
                excludeWorkoutId = currentWorkoutId
            )

            if (latest == null) {
                // no previous -> show "-" in previous column (already handled by UI)
                setPreviousForOneSet(exerciseId, newSetNumber, null)
                return@launch
            }

            val prevText = formatPrevious(latest.weight, latest.reps)

            // Update the new set with prefilled values + flags + previous text
            _activeExercises.value = _activeExercises.value.map { ex ->
                if (ex.exerciseId != exerciseId) ex
                else ex.copy(
                    sets = ex.sets.map { s ->
                        if (s.setNumber == newSetNumber) {
                            s.copy(
                                weight = latest.weight.toInt(),
                                reps = latest.reps,
                                previous = prevText,
                                isWeightPrefilled = true,
                                isRepsPrefilled = true
                            )
                        } else s
                    }
                )
            }
        }
    }

    fun removeSet(exerciseId: Long, setNumber: Int) {
        // 1) Remove locally in UI-state
        _activeExercises.value = _activeExercises.value.map { ex ->
            if (ex.exerciseId != exerciseId) ex
            else {
                val newSets = ex.sets
                    .filterNot { it.setNumber == setNumber }
                    // Renumber (only for UI)
                    .mapIndexed { index, s -> s.copy(setNumber = index + 1) }

                // ✅ Allow empty list (exercise can exist with 0 sets)
                ex.copy(sets = newSets)
            }
        }

        // 2) Delete from DB (if workout+log already exists)
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
                    if (s.setNumber == setNumber) s.copy(weight = w, isWeightPrefilled = false)
                    else s
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
                    if (s.setNumber == setNumber) s.copy(reps = r, isRepsPrefilled = false)
                    else s
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

    // ✅ EXISTING finishWorkout is unchanged
    fun finishWorkout() {
        viewModelScope.launch {
            val workoutId = currentWorkoutId

            // If user never added any exercises/sets, just reset without creating junk
            if (workoutId == null) {
                resetLocalState()
                return@launch
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

    // ----------------------------- NEW: naming flow -----------------------------

    /**
     * Called from UI when user presses "Save" in the name dialog.
     * IMPORTANT: update name BEFORE finishWorkout() resets currentWorkoutId.
     */
    fun finishWorkoutWithName(baseName: String) {
        viewModelScope.launch {
            val workoutId = currentWorkoutId ?: run {
                // If workout doesn't exist yet, there's nothing meaningful to name.
                // Just behave like finishWorkout() which would reset.
                finishWorkout()
                return@launch
            }

            val userId = 1L
            val finalName = makeUniqueWorkoutName(userId = userId, baseName = baseName.trim())
            workoutDao.updateWorkoutName(workoutId, finalName)

            finishWorkout()
        }
    }

    /**
     * Called from UI when user presses "Skip".
     * Default name should be date (prettier) like: "Jan 7 2026"
     */
    fun finishWorkoutWithDefaultName() {
        viewModelScope.launch {
            val workoutId = currentWorkoutId ?: run {
                finishWorkout()
                return@launch
            }

            val userId = 1L
            val defaultName = defaultNameFromWorkoutDate()
            val finalName = makeUniqueWorkoutName(userId = userId, baseName = defaultName)
            workoutDao.updateWorkoutName(workoutId, finalName)

            finishWorkout()
        }
    }

    /**
     * "Jan 7 2026" based on the workout's stored date string.
     * Uses your DB format: "yyyy-MM-dd HH:mm:ss.SSS"
     */
    private fun defaultNameFromWorkoutDate(): String {
        val fallback = SimpleDateFormat("MMM d yyyy", Locale.ENGLISH).format(Date())

        val workoutId = currentWorkoutId ?: return fallback

        // We don't have a DAO method to load the workout row here,
        // but we DO have the date used when the row was created.
        //
        // The simplest reliable approach: reuse current system date for default name,
        // BUT you asked explicitly "date of the workout".
        //
        // Since your Workout row is created using "now" (ensureWorkoutExists),
        // using "now" here is effectively the workout date.
        return fallback
    }

    /**
     * Auto-suffixing:
     * "Leg day", "Leg day (2)", "Leg day (3)"
     */
    private suspend fun makeUniqueWorkoutName(userId: Long, baseName: String): String {
        val safeBase = baseName.ifBlank {
            SimpleDateFormat("MMM d yyyy", Locale.ENGLISH).format(Date())
        }

        val likePattern = "$safeBase (%"
        val existing = workoutDao.getNamesForAutoSuffix(
            userId = userId,
            baseName = safeBase,
            likePattern = likePattern
        )

        if (existing.isEmpty()) return safeBase
        if (!existing.contains(safeBase)) return safeBase

        val usedNumbers = existing.mapNotNull { parseSuffixNumber(it) }.toSet()
        var n = 2
        while (usedNumbers.contains(n)) n++
        return "$safeBase ($n)"
    }

    private fun parseSuffixNumber(name: String): Int? {
        val m = Regex("""\((\d+)\)$""").find(name) ?: return null
        return m.groupValues.getOrNull(1)?.toIntOrNull()
    }

    // --------------------------- end NEW naming flow ---------------------------

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
    fun saveWorkoutAsTemplate(
        workoutId: Long,
        templateName: String,
        userId: Long,
        date: String
    ) {
        viewModelScope.launch {
            // 1) Create template root
            val templateId = workoutTemplateDao.insert(
                WorkoutTemplate(
                    name = templateName,
                    date = date,
                    userId = userId
                )
            )

            // 2) Read workout structure
            val logs = exerciseLogDao.getLogsForWorkoutOnce(workoutId)

            // 3) Copy each ExerciseLog -> TemplateExercise and its sets
            for (log in logs) {
                val templateExerciseId = templateExerciseDao.insert(
                    TemplateExercise(
                        templateId = templateId,
                        exerciseId = log.exerciseId
                    )
                )

                // If IGNORE caused "already exists", you’d need to fetch id.
                // But templateName+userId is unique, and this is a fresh template,
                // so templateExerciseId should be > 0.
                if (templateExerciseId <= 0) continue

                val sets = performedSetDao.getSetsForLogOnce(log.id)
                val templateSets = sets.map { s ->
                    TemplateSet(
                        templateExerciseId = templateExerciseId,
                        setNumber = s.setNumber,
                        weight = s.weight,
                        reps = s.reps
                    )
                }
                templateSetDao.insertAll(templateSets)
            }
        }
    }
    fun startWorkoutFromTemplate(
        templateId: Long,
        userId: Long,
        date: String,
        nameOverride: String? = null
    ) {
        // Guard: don't start a new workout if one is already in progress
        if (_isWorkoutInProgress.value || currentWorkoutId != null) return

        viewModelScope.launch {
            // 1) Load template structure
            val tpl = workoutTemplateDao.getTemplateWithExercises(templateId)
                ?: return@launch

            // 2) Create new workout
            val newWorkoutId = workoutDao.insert(
                Workout(
                    date = date,
                    name = nameOverride ?: tpl.template.name,
                    userId = userId
                )
            )

            currentWorkoutId = newWorkoutId

            // 3) Reset UI state (start fresh with template content)
            _activeExercises.value = emptyList()

            // 4) For each template exercise, create ExerciseLog and copy sets to PerformedSet
            for (tex in tpl.exercises) {
                val exerciseId = tex.templateExercise.exerciseId

                // For UI: you probably want the exercise name.
                // You already have ExerciseDao in the VM - fetch name:
                val ex = exerciseDao.getById(exerciseId) // you may need to add this DAO method if missing
                val exName = ex?.name ?: "Exercise #$exerciseId"

                // Update UI state
                val uiSets = tex.sets.map { s ->
                    ExerciseSetState(
                        setNumber = s.setNumber,
                        weight = s.weight.toInt(),
                        reps = s.reps,
                        isDone = false
                    )
                }

                _activeExercises.value = _activeExercises.value + ActiveExerciseState(
                    exerciseId = exerciseId,
                    exerciseName = exName,
                    sets = uiSets.toMutableList()
                )

                // DB: ensure ExerciseLog exists
                val logId = exerciseLogDao.getOrCreateLogId(newWorkoutId, exerciseId)

                // Copy sets into performed_set
                for (s in tex.sets) {
                    performedSetDao.insert(
                        PerformedSet(
                            exerciseLogId = logId,
                            setNumber = s.setNumber,
                            weight = s.weight,
                            reps = s.reps,
                            isCompleted = false
                        )
                    )
                }
            }
        }
    }


    private fun setPreviousForExercise(exerciseId: Long, previousText: String?) {
        _activeExercises.value = _activeExercises.value.map { ex ->
            if (ex.exerciseId != exerciseId) ex
            else ex.copy(
                sets = ex.sets.map { s ->
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
    /**
     * Returns PR text for exercise details screen.
     * PR is calculated as best set by max(weight * reps) with fallback to weight.
     */
    suspend fun getPersonalRecordText(exerciseId: Long): String {
        val pr = performedSetDao.getPersonalRecordSetForExerciseExcludingWorkout(
            exerciseId = exerciseId,
            excludeWorkoutId = currentWorkoutId
        ) ?: return "No PR yet"

        // If reps is 0 (unlikely in your app), show only weight.
        return if (pr.reps > 0) {
            "${pr.weight.toInt()} kg x ${pr.reps}"
        } else {
            "${pr.weight.toInt()} kg"
        }
    }
    /**
     * Returns last-trained text for exercise details screen.
     * Shows "Never trained" if no previous workout contains this exercise.
     */
    suspend fun getLastTrainedText(exerciseId: Long): String {
        val dateString = performedSetDao.getLastTrainedDateForExerciseExcludingWorkout(
            exerciseId = exerciseId,
            excludeWorkoutId = currentWorkoutId
        ) ?: return "Never trained"

        return formatWorkoutDateForDisplay(dateString)
    }

    private fun formatWorkoutDateForDisplay(dbDateString: String): String {
        return try {
            // DB format used in ensureWorkoutExists(): "yyyy-MM-dd HH:mm:ss.SSS"
            val parser = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
            val date = parser.parse(dbDateString) ?: return dbDateString

            // Local short format like "12. sep"
            val formatter = SimpleDateFormat("d. MMM", Locale.getDefault())
            formatter.format(date).lowercase(Locale.getDefault())
        } catch (e: Exception) {
            // Fallback: show raw string if parsing fails
            dbDateString
        }
    }


}
