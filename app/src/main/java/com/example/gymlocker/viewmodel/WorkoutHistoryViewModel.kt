package com.example.gymlocker.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.gymlocker.data.auth.SessionManager
import com.example.gymlocker.data.database.AppDatabase
import com.example.gymlocker.data.entity.PerformedSet
import com.example.gymlocker.data.entity.template.TemplateExercise
import com.example.gymlocker.data.entity.template.TemplateSet
import com.example.gymlocker.data.entity.template.WorkoutTemplate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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

    private val session by lazy { SessionManager(appContext) }

    fun completedWorkouts() = workoutDao.getWorkoutSummaries()

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

    suspend fun createTemplateFromWorkout(workoutId: Long, templateName: String): Long {
        val profileUserId = session.activeProfileUserIdFlowOnce()
            ?: throw IllegalStateException("No active profile selected")

        val logs = exerciseLogDao.getLogsForWorkoutOnce(workoutId)

        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val template = WorkoutTemplate(
            date = dateFormat.format(Date()),
            name = templateName,
            userId = profileUserId
        )

        val templateId = workoutTemplateDao.insert(template)

        logs.forEach { log ->
            val templateEx = TemplateExercise(
                templateId = templateId,
                exerciseId = log.exerciseId
            )
            val templateExerciseId = templateExerciseDao.insert(templateEx)

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

        return templateId
    }

    private suspend fun SessionManager.activeProfileUserIdFlowOnce(): Long? {
        var latest: Long? = null
        activeProfileUserId.collect { v ->
            latest = v
            return@collect
        }
        return latest
    }

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
