package com.example.gymlocker.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.gymlocker.data.database.AppDatabase
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import com.example.gymlocker.data.entity.*
import java.time.DayOfWeek
import com.example.gymlocker.data.entity.template.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import com.example.gymlocker.data.dao.MuscleGroupDistributionRow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import com.example.gymlocker.data.dao.WorkoutSummary
import kotlinx.coroutines.flow.map
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.flow.firstOrNull
import java.time.temporal.TemporalAdjusters
import com.example.gymlocker.data.auth.SessionManager
import com.example.gymlocker.notifications.RestTimerAlarm
import kotlinx.coroutines.ExperimentalCoroutinesApi
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.Date
import java.util.Locale


// Ét sæt (1 række i tabellen)
data class ExerciseSetState(
    val setNumber: Int,
    val weight: Int = 0,
    val reps: Int = 0,
    val isDone: Boolean = false,
    val previous: String? = null,

    // visual hint flags for opacity
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

data class WeekHoursUi(
    val weekStart: LocalDate,   // Monday
    val hours: Float
)

data class WeekVolumeUi(
    val weekStart: LocalDate, // Monday
    val volume: Float
)

/**
 * Stable UI model for Exercise details popup (and future screen reuse).
 */
data class ExerciseStatsUi(
    val prText: String,
    val lastTrainedText: String
)

data class RestTimerState(
    val isActive: Boolean = false,
    val exerciseId: Long? = null,
    val exerciseName: String? = null,
    val totalSeconds: Int = 0,
    val endTimeMillis: Long = 0L,
    val remainingSeconds: Int = 0,
    val remainingText: String = "0:00"
)

class ActiveWorkoutViewModel(app: Application) : AndroidViewModel(app) {
    private val appContext = app.applicationContext
    private val db by lazy { AppDatabase.getDatabase(appContext) }
    private val workoutDao by lazy { db.workoutDao() }
    private val exerciseLogDao by lazy { db.exerciseLogDao() }
    private val exerciseDao by lazy { db.exerciseDao() }
    private val performedSetDao by lazy { db.performedSetDao() }

    private val workoutTemplateDao by lazy { db.workoutTemplateDao() }
    private val templateExerciseDao by lazy { db.templateExerciseDao() }
    private val templateSetDao by lazy { db.templateSetDao() }

    // ✅ session holds active profile id (phase 1)
    private val session by lazy { SessionManager(appContext) }

    private var timerJob: Job? = null
    private var currentWorkoutId: Long? = null
    private val workoutCreateMutex = Mutex()

    private val _elapsedTime = MutableStateFlow(0L)
    val elapsedTime: StateFlow<Long> = _elapsedTime.asStateFlow()

    private val _isWorkoutInProgress = MutableStateFlow(false)
    val isWorkoutInProgress: StateFlow<Boolean> = _isWorkoutInProgress.asStateFlow()

    private val _activeExercises = MutableStateFlow<List<ActiveExerciseState>>(emptyList())
    val activeExercises: StateFlow<List<ActiveExerciseState>> = _activeExercises.asStateFlow()

    // Rest timer
    private val restPrefDao by lazy { db.exerciseRestPreferenceDao() }

    private var restTimerJob: Job? = null
    private val _restTimerState = MutableStateFlow(RestTimerState())
    val restTimerState: StateFlow<RestTimerState> = _restTimerState.asStateFlow()

    suspend fun readDefaultRestSeconds(exerciseId: Long): Int? {
        val uid = activeUserIdOrNull() ?: return null
        return restPrefDao.getRestSeconds(uid, exerciseId)
    }


    @OptIn(ExperimentalCoroutinesApi::class)
    fun completedWorkouts() =
        session.activeProfileUserId.flatMapLatest { userId ->
            if (userId == null) flowOf(emptyList())
            else workoutDao.getWorkoutSummariesForUser(userId)
        }

    /**
     * ✅ FIX: Last-workout label must use the same profile-scoped list.
     */
    fun lastWorkoutLabel() =
        completedWorkouts().map { workouts ->
            makeLastWorkoutLabel(workouts)
        }

    private fun makeLastWorkoutLabel(workouts: List<WorkoutSummary>): String {
        if (workouts.isEmpty()) return "Ingen tidligere workouts — klar til din første? 🚀"

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
     * ✅ Templates now depend on selected profile.
     * If no profile selected -> empty list.
     */
    fun observeTemplates(userId: Long?) =
        if (userId == null) flowOf(emptyList()) else workoutTemplateDao.observeTemplates(userId)

    /**
     * Fetch a specific template with its exercises for the detail screen.
     */
    suspend fun getTemplateWithExercises(templateId: Long) =
        workoutTemplateDao.getTemplateWithExercises(templateId)

    /**
     * Toggle favorite status for a template.
     */
    suspend fun toggleTemplateFavorite(templateId: Long) {
        workoutTemplateDao.toggleFavorite(templateId)
    }

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
    private suspend fun activeUserIdOrNull(): Long? {
        return session.activeProfileUserId.firstOrNull()
    }




    fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
    }

    fun discardWorkout() {
        val workoutIdToDelete = currentWorkoutId
        resetLocalState()

        viewModelScope.launch {
            workoutIdToDelete?.let { workoutDao.deleteById(it) }
        }
    }

    private fun resetLocalState() {
        skipRestTimer(cancelAlarm = true)
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

    private fun formatMmSs(totalSeconds: Int): String {
        val m = (totalSeconds / 60).coerceAtLeast(0)
        val s = (totalSeconds % 60).coerceAtLeast(0)
        return "%d:%02d".format(m, s)
    }

    fun formatRestSeconds(seconds: Int): String = formatMmSs(seconds.coerceAtLeast(0))

    // --- Core persistence helpers ---

    private suspend fun ensureWorkoutExists(): Long? = workoutCreateMutex.withLock {
        val existing = currentWorkoutId
        if (existing != null) return existing

        val userId = requireActiveProfileUserIdOrNull() ?: return null

        val dateString = SimpleDateFormat(
            "yyyy-MM-dd HH:mm:ss.SSS",
            Locale.getDefault()
        ).format(Date())

        val name = "Workout $dateString"

        val workoutId = workoutDao.insert(
            Workout(
                date = dateString,
                name = name,
                userId = userId,
                time = 0L
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
            muscleGroupId = exercise.muscleGroupId,
            sets = listOf(ExerciseSetState(setNumber = 1))
        )

        viewModelScope.launch {
            val latestWorkoutId = performedSetDao.getLatestWorkoutIdForExerciseExcludingWorkout(
                exerciseId = exercise.exerciseId,
                excludeWorkoutId = currentWorkoutId
            ) ?: return@launch

            val previousSets = performedSetDao.getPerformedSetsForExerciseInWorkout(
                workoutId = latestWorkoutId,
                exerciseId = exercise.exerciseId
            )

            if (previousSets.isEmpty()) return@launch

            val clonedSets = previousSets.map { ps ->
                ExerciseSetState(
                    setNumber = ps.setNumber,
                    weight = ps.weight.toInt(),
                    reps = ps.reps,
                    isDone = false,
                    previous = formatPrevious(ps.weight, ps.reps),
                    isWeightPrefilled = true,
                    isRepsPrefilled = true
                )
            }

            _activeExercises.value = _activeExercises.value.map { ex ->
                if (ex.exerciseId != exercise.exerciseId) ex
                else ex.copy(sets = clonedSets)
            }
        }

        viewModelScope.launch {
            val workoutId = ensureWorkoutExists() ?: return@launch
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
                setPreviousForOneSet(exerciseId, newSetNumber, null)
                return@launch
            }

            val prevText = formatPrevious(latest.weight, latest.reps)

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
        _activeExercises.value = _activeExercises.value.map { ex ->
            if (ex.exerciseId != exerciseId) ex
            else {
                val newSets = ex.sets
                    .filterNot { it.setNumber == setNumber }
                    .mapIndexed { index, s -> s.copy(setNumber = index + 1) }
                ex.copy(sets = newSets)
            }
        }

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

    fun markAllSetsDone(exerciseId: Long) {
        val before = _activeExercises.value.firstOrNull { it.exerciseId == exerciseId } ?: return
        val setsBefore = before.sets

        _activeExercises.value = _activeExercises.value.map { ex ->
            if (ex.exerciseId != exerciseId) ex
            else ex.copy(sets = ex.sets.map { it.copy(isDone = true) })
        }

        viewModelScope.launch {
            val workoutId = ensureWorkoutExists() ?: return@launch
            val logId = exerciseLogDao.getOrCreateLogId(workoutId, exerciseId)

            setsBefore
                .filter { it.weight > 0 && it.reps > 0 }
                .forEach { s ->
                    performedSetDao.upsertByNumber(
                        exerciseLogId = logId,
                        setNumber = s.setNumber,
                        weight = s.weight.toFloat(),
                        reps = s.reps,
                        isCompleted = true
                    )
                }
        }
    }

    fun hasUnfinishedMeaningfulSets(): Boolean {
        return _activeExercises.value.any { ex ->
            ex.sets.any { s -> s.weight > 0 && s.reps > 0 && !s.isDone }
        }
    }

    fun markAllUnfinishedMeaningfulSetsDone() {
        val snapshot = _activeExercises.value

        // Kun markér dem der faktisk er unfinished
        _activeExercises.value = snapshot.map { ex ->
            ex.copy(
                sets = ex.sets.map { s ->
                    if (s.weight > 0 && s.reps > 0 && !s.isDone) s.copy(isDone = true) else s
                }
            )
        }

        viewModelScope.launch {
            val workoutId = ensureWorkoutExists() ?: return@launch

            snapshot.forEach { ex ->
                val logId = exerciseLogDao.getOrCreateLogId(workoutId, ex.exerciseId)

                // Kun upsert de sæt der var unfinished
                ex.sets
                    .filter { it.weight > 0 && it.reps > 0 && !it.isDone }
                    .forEach { s ->
                        performedSetDao.upsertByNumber(
                            exerciseLogId = logId,
                            setNumber = s.setNumber,
                            weight = s.weight.toFloat(),
                            reps = s.reps,
                            isCompleted = true
                        )
                    }
            }
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

        viewModelScope.launch {
            val workoutId = ensureWorkoutExists() ?: return@launch
            val logId = exerciseLogDao.getOrCreateLogId(workoutId, exerciseId)

            if (setBefore.reps > 0 && setBefore.weight > 0) {
                performedSetDao.upsertByNumber(
                    exerciseLogId = logId,
                    setNumber = setNumber,
                    weight = setBefore.weight.toFloat(),
                    reps = setBefore.reps,
                    isCompleted = isDone
                )
                if (isDone) {
                    val uid = activeUserIdOrNull() ?: return@launch
                    val seconds = getDefaultRestSeconds(userId = uid, exerciseId = exerciseId)

                    if (seconds != null && seconds > 0) {
                        startRestTimer(exerciseId, before.exerciseName, seconds)
                    }
                }
            }
        }
    }

    fun finishWorkout() {
        viewModelScope.launch {
            val workoutId = currentWorkoutId

            if (workoutId == null) {
                resetLocalState()
                return@launch
            }

            val snapshot = _activeExercises.value

            snapshot.forEach { ex ->
                val sets = meaningfulSets(ex)

                if (sets.isEmpty()) {
                    exerciseLogDao.getLogId(workoutId, ex.exerciseId)?.let { logId ->
                        performedSetDao.deleteSetsForLog(logId)
                        exerciseLogDao.deleteById(logId)
                    }
                    return@forEach
                }

                val logId = exerciseLogDao.getOrCreateLogId(workoutId, ex.exerciseId)

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

            // ✅ store workout duration (seconds)
            workoutDao.updateWorkoutTime(workoutId, _elapsedTime.value)

            resetLocalState()
        }
    }

    fun finishWorkoutWithName(baseName: String) {
        viewModelScope.launch {
            val workoutId = currentWorkoutId ?: run {
                finishWorkout()
                return@launch
            }

            val userId = requireActiveProfileUserIdOrNull()
                ?: run {
                    finishWorkout()
                    return@launch
                }

            val finalName = makeUniqueWorkoutName(userId = userId, baseName = baseName.trim())
            workoutDao.updateWorkoutName(workoutId, finalName)

            finishWorkout()
        }
    }

    fun finishWorkoutWithDefaultName() {
        viewModelScope.launch {
            val workoutId = currentWorkoutId ?: run {
                finishWorkout()
                return@launch
            }

            val userId = requireActiveProfileUserIdOrNull()
                ?: run {
                    finishWorkout()
                    return@launch
                }

            val defaultName = defaultNameFromWorkoutDate()
            val finalName = makeUniqueWorkoutName(userId = userId, baseName = defaultName)
            workoutDao.updateWorkoutName(workoutId, finalName)

            finishWorkout()
        }
    }

    private fun defaultNameFromWorkoutDate(): String {
        return SimpleDateFormat("MMM d yyyy", Locale.ENGLISH).format(Date())
    }

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

    companion object {
        const val MAX_WORKOUT_NAME_LENGTH = 40

        fun provideFactory(context: Context): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return ActiveWorkoutViewModel(context.applicationContext as Application) as T
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
            val effectiveUserId = if (userId > 0) userId else (requireActiveProfileUserIdOrNull() ?: return@launch)

            // 1) Create template root
            val templateId = workoutTemplateDao.insert(
                WorkoutTemplate(
                    name = templateName,
                    date = date,
                    userId = effectiveUserId
                )
            )

            val logs = exerciseLogDao.getLogsForWorkoutOnce(workoutId)

            for (log in logs) {
                val templateExerciseId = templateExerciseDao.insert(
                    TemplateExercise(
                        templateId = templateId,
                        exerciseId = log.exerciseId
                    )
                )

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
        if (_isWorkoutInProgress.value || currentWorkoutId != null) return

        viewModelScope.launch {
            val effectiveUserId = if (userId > 0) userId else (requireActiveProfileUserIdOrNull() ?: return@launch)

            // 1) Load template structure
            val tpl = workoutTemplateDao.getTemplateWithExercises(templateId)
                ?: return@launch

            val newWorkoutId = workoutDao.insert(
                Workout(
                    date = date,
                    name = nameOverride ?: tpl.template.name,
                    userId = effectiveUserId
                )
            )

            currentWorkoutId = newWorkoutId

            _activeExercises.value = emptyList()
            _elapsedTime.value = 0L

            _isWorkoutInProgress.value = true
            startTimer()

            for (tex in tpl.exercises) {
                val exerciseId = tex.templateExercise.exerciseId

                val ex = exerciseDao.getById(exerciseId)
                val exName = ex?.name ?: "Exercise #$exerciseId"
                val muscleGroupId = ex?.muscleGroupId ?: 0L

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
                    muscleGroupId = muscleGroupId,
                    sets = uiSets
                )

                val logId = exerciseLogDao.getOrCreateLogId(newWorkoutId, exerciseId)

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

    suspend fun getPersonalRecordText(exerciseId: Long): String {
        val pr = performedSetDao.getPersonalRecordSetForExerciseExcludingWorkout(
            exerciseId = exerciseId,
            excludeWorkoutId = currentWorkoutId
        ) ?: return "No PR yet"

        return if (pr.reps > 0) {
            "${pr.weight.toInt()} kg x ${pr.reps}"
        } else {
            "${pr.weight.toInt()} kg"
        }
    }

    suspend fun getLastTrainedText(exerciseId: Long): String {
        val dateString = performedSetDao.getLastTrainedDateForExerciseExcludingWorkout(
            exerciseId = exerciseId,
            excludeWorkoutId = currentWorkoutId
        ) ?: return "Never trained"

        return formatWorkoutDateForDisplay(dateString)
    }

    suspend fun getExerciseStatsUi(exerciseId: Long): ExerciseStatsUi {
        return try {
            ExerciseStatsUi(
                prText = getPersonalRecordText(exerciseId),
                lastTrainedText = getLastTrainedText(exerciseId)
            )
        } catch (e: Exception) {
            ExerciseStatsUi(
                prText = "No PR yet",
                lastTrainedText = "Never trained"
            )
        }
    }

    private fun formatWorkoutDateForDisplay(dbDateString: String): String {
        return try {
            val parser = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
            val date = parser.parse(dbDateString) ?: return dbDateString

            val formatter = SimpleDateFormat("d. MMM", Locale.getDefault())
            formatter.format(date).lowercase(Locale.getDefault())
        } catch (e: Exception) {
            dbDateString
        }
    }

    fun deleteTemplateExerciseById(templateExerciseId: Long) {
        viewModelScope.launch {
            templateExerciseDao.deleteById(templateExerciseId)
        }
    }

    // Rest timer
    private suspend fun getDefaultRestSeconds(userId: Long, exerciseId: Long): Int? {
        return restPrefDao.getRestSeconds(userId, exerciseId)
    }

    fun setDefaultRestSeconds(exerciseId: Long, restSeconds: Int) {
        viewModelScope.launch {
            val uid = activeUserIdOrNull() ?: return@launch
            val clamped = restSeconds.coerceIn(0, 60 * 30)

            if (clamped <= 0) restPrefDao.delete(uid, exerciseId)
            else restPrefDao.upsert(
                ExerciseRestPreference(
                    userId = uid,
                    exerciseId = exerciseId,
                    restSeconds = clamped
                )
            )
        }
    }


    fun skipRestTimer(cancelAlarm: Boolean = true) {
        if (cancelAlarm) {
            RestTimerAlarm.cancel(appContext)
        }
        restTimerJob?.cancel()
        restTimerJob = null
        _restTimerState.value = RestTimerState()
    }

    private fun startRestTimer(exerciseId: Long, exerciseName: String, seconds: Int) {
        if (seconds <= 0) return

        restTimerJob?.cancel()

        val endAt = System.currentTimeMillis() + seconds * 1000L

        // Cancel evt. gammel alarm, så vi ikke får stale notifikationer
        RestTimerAlarm.cancel(appContext)

        // Planlæg notifikation når timeren udløber
        RestTimerAlarm.schedule(
            context = appContext,
            triggerAtMillis = endAt,
            exerciseName = exerciseName
        )

        _restTimerState.value = RestTimerState(
            isActive = true,
            exerciseId = exerciseId,
            exerciseName = exerciseName,
            totalSeconds = seconds,
            endTimeMillis = endAt,
            remainingSeconds = seconds,
            remainingText = formatMmSs(seconds)
        )

        restTimerJob = viewModelScope.launch {
            while (true) {
                val remaining = ((endAt - System.currentTimeMillis()) / 1000L)
                    .toInt()
                    .coerceAtLeast(0)

                _restTimerState.value = _restTimerState.value.copy(
                    remainingSeconds = remaining,
                    remainingText = formatMmSs(remaining)
                )

                if (remaining <= 0) {
                    // Alarmen har (eller bør) fyre nu – cancel ikke den her
                    skipRestTimer(cancelAlarm = false)
                    return@launch
                }

                delay(250L)
            }
        }
    }

    /**
     * ✅ Session helper: returns active profile or null.
     * Using firstOrNull avoids blocking the coroutine.
     */
    private suspend fun requireActiveProfileUserIdOrNull(): Long? {
        return session.activeProfileUserId.firstOrNull()
    }

    // ----------------------------
    // Stats: weekly hours / volume
    // ----------------------------

}