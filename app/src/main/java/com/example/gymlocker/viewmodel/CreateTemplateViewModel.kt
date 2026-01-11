package com.example.gymlocker.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.gymlocker.data.database.AppDatabase
import com.example.gymlocker.data.entity.Exercises
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

// Represents a template exercise with its sets
data class TemplateExerciseState(
    val templateExerciseId: Long = 0L,
    val exerciseId: Long,
    val exerciseName: String,
    val muscleGroupId: Long,
    val sets: List<TemplateSetState> = listOf(TemplateSetState(setNumber = 1))
)

data class TemplateSetState(
    val setNumber: Int,
    val weight: Float = 0f,
    val reps: Int = 0
)

class CreateTemplateViewModel(private val appContext: Context) : ViewModel() {

    private val db by lazy { AppDatabase.getDatabase(appContext) }
    private val workoutTemplateDao by lazy { db.workoutTemplateDao() }
    private val templateExerciseDao by lazy { db.templateExerciseDao() }
    private val templateSetDao by lazy { db.templateSetDao() }

    private val _templateName = MutableStateFlow("")
    val templateName: StateFlow<String> = _templateName.asStateFlow()

    // ✅ Error message for live validation
    private val _templateNameError = MutableStateFlow<String?>(null)
    val templateNameError: StateFlow<String?> = _templateNameError.asStateFlow()

    private val _selectedExercises = MutableStateFlow<List<TemplateExerciseState>>(emptyList())
    val selectedExercises: StateFlow<List<TemplateExerciseState>> = _selectedExercises.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    /**
     * Live validation:
     * - Blank => error
     * - Too long => error
     * - Otherwise => ok
     *
     * Hard max-length: we keep previous value if the user tries to exceed MAX_TEMPLATE_NAME_LENGTH.
     */
    fun updateTemplateName(name: String) {
        if (name.length <= MAX_TEMPLATE_NAME_LENGTH) {
            _templateName.value = name
        }
        val current = _templateName.value

        _templateNameError.value = when {
            current.isBlank() -> "Please enter a name."
            current.length > MAX_TEMPLATE_NAME_LENGTH -> "Max $MAX_TEMPLATE_NAME_LENGTH characters."
            else -> null
        }
    }

    fun addExercise(exercise: Exercises) {
        val currentList = _selectedExercises.value.toMutableList()
        if (!currentList.any { it.exerciseId == exercise.exerciseId }) {
            currentList.add(
                TemplateExerciseState(
                    exerciseId = exercise.exerciseId,
                    exerciseName = exercise.name,
                    muscleGroupId = exercise.muscleGroupId,
                    sets = listOf(TemplateSetState(setNumber = 1))
                )
            )
            _selectedExercises.value = currentList
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

        // Validate one last time (also sets the correct error message)
        updateTemplateName(name)

        if (_templateNameError.value != null) return
        if (_selectedExercises.value.isEmpty()) return

        viewModelScope.launch {
            _isSaving.value = true
            try {
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val currentDate = dateFormat.format(Date())

                val templateId = workoutTemplateDao.insert(
                    WorkoutTemplate(
                        date = currentDate,
                        name = name,
                        userId = 1L // TODO: Use actual user ID
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
