package com.example.gymlocker.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.gymlocker.data.database.AppDatabase
import com.example.gymlocker.data.entity.Exercises
import com.example.gymlocker.data.entity.template.TemplateExercise
import com.example.gymlocker.data.entity.template.TemplateSet
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class EditTemplateViewModel(
    private val appContext: Context,
    private val templateId: Long
) : ViewModel() {

    private val db by lazy { AppDatabase.getDatabase(appContext) }
    private val workoutTemplateDao by lazy { db.workoutTemplateDao() }
    private val templateExerciseDao by lazy { db.templateExerciseDao() }
    private val templateSetDao by lazy { db.templateSetDao() }
    private val exerciseDao by lazy { db.exerciseDao() }

    private val _templateName = MutableStateFlow("")
    val templateName: StateFlow<String> = _templateName.asStateFlow()

    private val _templateNameError = MutableStateFlow<String?>(null)
    val templateNameError: StateFlow<String?> = _templateNameError.asStateFlow()

    private val _selectedExercises = MutableStateFlow<List<TemplateExerciseState>>(emptyList())
    val selectedExercises: StateFlow<List<TemplateExerciseState>> = _selectedExercises.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    init {
        loadTemplate()
    }

    private fun loadTemplate() {
        viewModelScope.launch {
            try {
                _isLoading.value = true

                val template = workoutTemplateDao.getById(templateId)
                if (template != null) {
                    _templateName.value = template.name
                }

                val templateExercises = templateExerciseDao.getByTemplateOnce(templateId)
                val exercisesState = mutableListOf<TemplateExerciseState>()

                for (te in templateExercises) {
                    val exercise = exerciseDao.getById(te.exerciseId)
                    val sets = templateSetDao.getByTemplateExerciseOnce(te.id)

                    if (exercise != null) {
                        exercisesState.add(
                            TemplateExerciseState(
                                exerciseId = te.exerciseId,
                                exerciseName = exercise.name,
                                sets = sets.map {
                                    TemplateSetState(
                                        setNumber = it.setNumber,
                                        weight = it.weight,
                                        reps = it.reps
                                    )
                                }
                            )
                        )
                    }
                }

                _selectedExercises.value = exercisesState
            } finally {
                _isLoading.value = false
            }
        }
    }

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
        updateTemplateName(name)

        if (_templateNameError.value != null) return
        if (_selectedExercises.value.isEmpty()) return

        viewModelScope.launch {
            _isSaving.value = true
            try {
                val template = workoutTemplateDao.getById(templateId)
                if (template != null) {
                    workoutTemplateDao.update(template.copy(name = name))
                }

                val existingExercises = templateExerciseDao.getByTemplateOnce(templateId)
                val existingExerciseIds = existingExercises.associateBy { it.exerciseId }

                // Process each exercise in the current state
                _selectedExercises.value.forEach { stateExercise ->
                    val existingTemplateExercise = existingExerciseIds[stateExercise.exerciseId]

                    if (existingTemplateExercise == null) {
                        // New exercise - insert it and its sets
                        val newTemplateExerciseId = templateExerciseDao.insert(
                            TemplateExercise(
                                templateId = templateId,
                                exerciseId = stateExercise.exerciseId
                            )
                        )

                        val templateSets = stateExercise.sets.map { set ->
                            TemplateSet(
                                templateExerciseId = newTemplateExerciseId,
                                setNumber = set.setNumber,
                                weight = set.weight,
                                reps = set.reps
                            )
                        }
                        templateSetDao.insertAll(templateSets)
                    } else {
                        // Existing exercise - delete old sets and insert new ones
                        templateSetDao.deleteByTemplateExerciseId(existingTemplateExercise.id)

                        val templateSets = stateExercise.sets.map { set ->
                            TemplateSet(
                                templateExerciseId = existingTemplateExercise.id,
                                setNumber = set.setNumber,
                                weight = set.weight,
                                reps = set.reps
                            )
                        }
                        templateSetDao.insertAll(templateSets)
                    }
                }

                // Remove exercises that are no longer in the state
                val stateExerciseIds = _selectedExercises.value.map { it.exerciseId }.toSet()
                existingExercises.forEach { existingExercise ->
                    if (existingExercise.exerciseId !in stateExerciseIds) {
                        templateExerciseDao.deleteById(existingExercise.id)
                    }
                }
            } finally {
                _isSaving.value = false
            }
        }
    }

    companion object {
        const val MAX_TEMPLATE_NAME_LENGTH = 40

        fun provideFactory(context: Context, templateId: Long): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return EditTemplateViewModel(context.applicationContext, templateId) as T
                }
            }
        }
    }
}

