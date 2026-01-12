package com.example.gymlocker.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.gymlocker.data.auth.SessionManager
import com.example.gymlocker.data.database.AppDatabase
import com.example.gymlocker.data.entity.template.TemplateExercise
import com.example.gymlocker.data.entity.template.TemplateSet
import com.example.gymlocker.data.entity.template.WorkoutTemplate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class TemplateSetState(
    val setNumber: Int,
    val weight: Float = 0f,
    val reps: Int = 0
)

data class TemplateExerciseState(
    val exerciseId: Long,
    val exerciseName: String,
    val sets: List<TemplateSetState> = listOf(TemplateSetState(setNumber = 1))
)

class CreateTemplateViewModel(private val appContext: Context) : ViewModel() {

    private val db by lazy { AppDatabase.getDatabase(appContext) }
    private val workoutTemplateDao by lazy { db.workoutTemplateDao() }
    private val templateExerciseDao by lazy { db.templateExerciseDao() }
    private val templateSetDao by lazy { db.templateSetDao() }
    private val exerciseDao by lazy { db.exerciseDao() }

    private val session by lazy { SessionManager(appContext) }

    private val _templateName = MutableStateFlow("")
    val templateName: StateFlow<String> = _templateName.asStateFlow()

    private val _templateNameError = MutableStateFlow<String?>(null)
    val templateNameError: StateFlow<String?> = _templateNameError.asStateFlow()

    private val _selectedExercises = MutableStateFlow<List<TemplateExerciseState>>(emptyList())
    val selectedExercises: StateFlow<List<TemplateExerciseState>> = _selectedExercises.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    fun updateTemplateName(value: String) {
        _templateName.value = value
        _templateNameError.value = when {
            value.isBlank() -> "Name cannot be empty"
            value.length > MAX_TEMPLATE_NAME_LENGTH -> "Max $MAX_TEMPLATE_NAME_LENGTH characters"
            else -> null
        }
    }

    fun addExercise(exerciseId: Long) {
        viewModelScope.launch {
            val exerciseName = exerciseDao.getById(exerciseId)?.name ?: "Unknown"
            if (_selectedExercises.value.any { it.exerciseId == exerciseId }) return@launch
            _selectedExercises.value = _selectedExercises.value + TemplateExerciseState(
                exerciseId = exerciseId,
                exerciseName = exerciseName
            )
        }
    }

    fun removeExercise(exerciseId: Long) {
        val currentList = _selectedExercises.value.toMutableList()
        currentList.removeAll { it.exerciseId == exerciseId }
        _selectedExercises.value = currentList
    }

    fun addSet(exerciseId: Long) {
        _selectedExercises.value = _selectedExercises.value.map { exercise ->
            if (exercise.exerciseId != exerciseId) {
                exercise
            } else {
                val newSetNumber = (exercise.sets.maxOfOrNull { it.setNumber } ?: 0) + 1
                exercise.copy(
                    sets = exercise.sets + TemplateSetState(setNumber = newSetNumber)
                )
            }
        }
    }

    fun removeSet(exerciseId: Long, setNumber: Int) {
        _selectedExercises.value = _selectedExercises.value.map { exercise ->
            if (exercise.exerciseId != exerciseId) {
                exercise
            } else {
                val kept = exercise.sets.filterNot { it.setNumber == setNumber }
                val reNumbered = kept.mapIndexed { index, set -> set.copy(setNumber = index + 1) }
                exercise.copy(sets = reNumbered)
            }
        }
    }

    fun updateSetWeight(exerciseId: Long, setNumber: Int, weight: String) {
        _selectedExercises.value = _selectedExercises.value.map { exercise ->
            if (exercise.exerciseId != exerciseId) {
                exercise
            } else {
                exercise.copy(
                    sets = exercise.sets.map { set ->
                        if (set.setNumber != setNumber) set
                        else set.copy(weight = weight.toFloatOrNull() ?: 0f)
                    }
                )
            }
        }
    }

    fun updateSetReps(exerciseId: Long, setNumber: Int, reps: String) {
        _selectedExercises.value = _selectedExercises.value.map { exercise ->
            if (exercise.exerciseId != exerciseId) {
                exercise
            } else {
                exercise.copy(
                    sets = exercise.sets.map { set ->
                        if (set.setNumber != setNumber) set
                        else set.copy(reps = reps.toIntOrNull() ?: 0)
                    }
                )
            }
        }
    }

    fun saveTemplate() {
        val name = _templateName.value.trim()
        updateTemplateName(name)

        if (_templateNameError.value != null) return
        if (_selectedExercises.value.isEmpty()) return

        viewModelScope.launch {
            _isSaving.value = true
            try {
                val profileUserId = session.activeProfileUserIdFlowOnce()
                    ?: return@launch // no profile yet -> do nothing (phase 2 will show UI message)

                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val currentDate = dateFormat.format(Date())

                val templateId = workoutTemplateDao.insert(
                    WorkoutTemplate(
                        date = currentDate,
                        name = name,
                        userId = profileUserId
                    )
                )

                _selectedExercises.value.forEach { templateExercise ->
                    val templateExerciseId = templateExerciseDao.insert(
                        TemplateExercise(
                            templateId = templateId,
                            exerciseId = templateExercise.exerciseId
                        )
                    )

                    val templateSets = templateExercise.sets.map { set ->
                        TemplateSet(
                            templateExerciseId = templateExerciseId,
                            setNumber = set.setNumber,
                            weight = set.weight,
                            reps = set.reps
                        )
                    }

                    templateSetDao.insertAll(templateSets)
                }

                _templateName.value = ""
                _templateNameError.value = null
                _selectedExercises.value = emptyList()
            } finally {
                _isSaving.value = false
            }
        }
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
        const val MAX_TEMPLATE_NAME_LENGTH = 40

        fun provideFactory(context: Context): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return CreateTemplateViewModel(context.applicationContext) as T
                }
            }
        }
    }
}
