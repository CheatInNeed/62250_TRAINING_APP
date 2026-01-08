package com.example.gymlocker.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.gymlocker.data.database.AppDatabase
import com.example.gymlocker.data.entity.PerformedSet
import com.example.gymlocker.data.entity.template.TemplateExercise
import com.example.gymlocker.data.entity.template.TemplateSet
import com.example.gymlocker.data.entity.template.WorkoutTemplate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.ExperimentalCoroutinesApi
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Detail view state
data class WorkoutLogDetail(
    val exerciseName: String,
    val sets: List<PerformedSet>
)

@OptIn(ExperimentalCoroutinesApi::class)
class WorkoutHistoryViewModel(private val appContext: Context) : ViewModel() {

    private val db by lazy { AppDatabase.getDatabase(appContext) }
    private val workoutDao by lazy { db.workoutDao() }
    private val exerciseLogDao by lazy { db.exerciseLogDao() }
    private val performedSetDao by lazy { db.performedSetDao() }
    private val exerciseDao by lazy { db.exerciseDao() }
    private val workoutTemplateDao by lazy { db.workoutTemplateDao() }
    private val templateExerciseDao by lazy { db.templateExerciseDao() }
    private val templateSetDao by lazy { db.templateSetDao() }

    /**
     * Used by WorkoutHistoryScreen
     */
    fun completedWorkouts() = workoutDao.getWorkoutSummaries()

    /**
     * Used by WorkoutDetailScreen
     */
    fun getWorkoutDetails(workoutId: Long): Flow<List<WorkoutLogDetail>> =
        exerciseLogDao.observeLogsForWorkout(workoutId).flatMapLatest { logs ->
            if (logs.isEmpty()) return@flatMapLatest flowOf(emptyList())

            val flows = logs.map { log ->
                val exerciseFlow = exerciseDao.getAllExercises().map { exercises ->
                    exercises.find { it.exerciseId == log.exerciseId }?.name ?: "Unknown Exercise"
                }
                val setsFlow = performedSetDao.observeSetsForLog(log.id)
                
                combine(exerciseFlow, setsFlow) { name, sets ->
                    WorkoutLogDetail(name, sets)
                }
            }
            combine(flows) { it.toList() }
        }

    /**
     * Create a template from a completed workout
     */
    suspend fun createTemplateFromWorkout(workoutId: Long, templateName: String): Long {
        // Get all exercise logs for this workout
        val logs = exerciseLogDao.getLogsForWorkoutOnce(workoutId)

        // Create the template
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val template = WorkoutTemplate(
            date = dateFormat.format(Date()),
            name = templateName,
            userId = 1L // TODO: Use actual user ID
        )

        val templateId = workoutTemplateDao.insert(template)

        // Add exercises and sets to the template
        logs.forEach { log ->
            val templateEx = TemplateExercise(
                templateId = templateId,
                exerciseId = log.exerciseId
            )
            val templateExerciseId = templateExerciseDao.insert(templateEx)

            // Get sets for this exercise log
            val sets = performedSetDao.getSetsByLogOnce(log.id)
            val templateSets = sets.map { set ->
                TemplateSet(
                    templateExerciseId = templateExerciseId,
                    setNumber = set.setNumber,
                    weight = set.weight.toFloat(),
                    reps = set.reps
                )
            }
            templateSetDao.insertAll(templateSets)
        }

        return templateId
    }

    // --- Factory ---

    companion object {
        fun provideFactory(context: Context): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return WorkoutHistoryViewModel(context.applicationContext) as T
                }
            }
        }
    }
}
