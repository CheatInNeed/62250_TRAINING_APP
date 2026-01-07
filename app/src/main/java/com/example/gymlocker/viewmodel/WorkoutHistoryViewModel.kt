package com.example.gymlocker.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.gymlocker.data.database.AppDatabase
import com.example.gymlocker.data.entity.PerformedSet
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

// Detail view state
data class WorkoutLogDetail(
    val exerciseName: String,
    val sets: List<PerformedSet>
)

class WorkoutHistoryViewModel(private val appContext: Context) : ViewModel() {

    private val db by lazy { AppDatabase.getDatabase(appContext) }
    private val workoutDao by lazy { db.workoutDao() }
    private val exerciseLogDao by lazy { db.exerciseLogDao() }
    private val performedSetDao by lazy { db.performedSetDao() }
    private val exerciseDao by lazy { db.exerciseDao() }

    /**
     * Used by WorkoutHistoryScreen
     */
    fun completedWorkouts() = workoutDao.getWorkoutSummaries()

    /**
     * Used by WorkoutDetailScreen
     */
    fun getWorkoutDetails(workoutId: Long): Flow<List<WorkoutLogDetail>> {
        return exerciseLogDao.observeLogsForWorkout(workoutId).flatMapLatest { logs ->
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
