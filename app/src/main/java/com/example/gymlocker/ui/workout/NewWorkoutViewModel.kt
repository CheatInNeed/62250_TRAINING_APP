package com.example.gymlocker.ui.workout

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gymlocker.data.dao.*
import com.example.gymlocker.data.entity.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class WorkoutSet(val reps: String, val weight: String)
data class WorkoutExercise(val name: String, val sets: List<WorkoutSet>)

class NewWorkoutViewModel(
    private val userDao: UserDao,
    private val workoutDao: WorkoutDao,
    private val exerciseDao: ExerciseDao,
    private val exerciseLogDao: ExerciseLogDao,
    private val userWorkoutCrossRefDao: UserWorkoutCrossRefDao,
    private val muscleGroupDao: MuscleGroupDao
) : ViewModel() {
    val exercises = mutableStateOf(listOf<WorkoutExercise>())

    private val _saveState = MutableStateFlow(false)
    val saveState: StateFlow<Boolean> = _saveState

    fun addExercise(name: String) {
        viewModelScope.launch {
            if (exercises.value.any { it.name == name }) return@launch

            val existingExercise = exerciseDao.getExerciseByName(name).firstOrNull()
            val lastLog = existingExercise?.let { exerciseLogDao.getLatestLogForExercise(it.exerciseId).firstOrNull() }

            val sets = if (lastLog != null) {
                listOf(WorkoutSet(lastLog.reps.toString(), lastLog.weight.toString()))
            } else {
                listOf()
            }

            exercises.value = exercises.value + WorkoutExercise(name, sets)
        }
    }

    fun addSet(exercise: WorkoutExercise, reps: String, weight: String) {
        val updatedSets = exercise.sets + WorkoutSet(reps, weight)
        val updatedExercise = exercise.copy(sets = updatedSets)
        exercises.value = exercises.value.map {
            if (it.name == exercise.name) updatedExercise else it
        }
    }

    fun saveWorkout(workoutName: String) {
        viewModelScope.launch {
            val defaultMuscleGroup = muscleGroupDao.getAllMuscleGroups().firstOrNull()?.firstOrNull() ?: MuscleGroup(muscleGroupId = 1, name = "Default").also { muscleGroupDao.insert(it) }
            val user = userDao.getUser(1).firstOrNull() ?: User(userId = 1, name = "Default User", height = 0, weight = 0).also { userDao.insert(it) }
            val workoutId = workoutDao.insert(Workout(name = workoutName, date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()), userId = user.userId))

            exercises.value.forEach { workoutExercise ->
                var exercise = exerciseDao.getExerciseByName(workoutExercise.name).firstOrNull()
                if (exercise == null) {
                    val newExerciseId = exerciseDao.insert(Exercises(name = workoutExercise.name, muscleGroupId = defaultMuscleGroup.muscleGroupId, startReps = 0, startWeight = 0, isRecent = true))
                    exercise = Exercises(exerciseId = newExerciseId, name = workoutExercise.name, muscleGroupId = defaultMuscleGroup.muscleGroupId, startReps = 0, startWeight = 0, isRecent = true)
                }
                
                userWorkoutCrossRefDao.insert(WorkoutExerciseCrossRef(workoutId, exercise.exerciseId))

                workoutExercise.sets.forEach { set ->
                    val repsAsInt = set.reps.toIntOrNull() ?: 0
                    val weightAsInt = set.weight.toIntOrNull() ?: 0
                    exerciseLogDao.insert(ExerciseLog(exerciseId = exercise.exerciseId, reps = repsAsInt, weight = weightAsInt, date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())))
                }
            }
            _saveState.value = true
        }
    }
}
