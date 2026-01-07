package com.example.gymlocker.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.gymlocker.data.database.AppDatabase
import com.example.gymlocker.data.entity.Exercises
import com.example.gymlocker.data.entity.template.TemplateExercise
import com.example.gymlocker.data.entity.template.WorkoutTemplate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CreateTemplateViewModel(private val appContext: Context) : ViewModel() {

    private val db by lazy { AppDatabase.getDatabase(appContext) }
    private val workoutTemplateDao by lazy { db.workoutTemplateDao() }
    private val templateExerciseDao by lazy { db.templateExerciseDao() }
    private val exerciseDao by lazy { db.exerciseDao() }

    private val _templateName = MutableStateFlow("")
    val templateName: StateFlow<String> = _templateName.asStateFlow()

    private val _selectedExercises = MutableStateFlow<List<Exercises>>(emptyList())
    val selectedExercises: StateFlow<List<Exercises>> = _selectedExercises.asStateFlow()

    private val _availableExercises = MutableStateFlow<List<Exercises>>(emptyList())
    val availableExercises: StateFlow<List<Exercises>> = _availableExercises.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    init {
        loadAvailableExercises()
    }

    private fun loadAvailableExercises() {
        viewModelScope.launch {
            exerciseDao.getAllExercises().collect { exercises ->
                _availableExercises.value = exercises
            }
        }
    }

    fun updateTemplateName(name: String) {
        _templateName.value = name
    }

    fun addExercise(exercise: Exercises) {
        val currentList = _selectedExercises.value.toMutableList()
        if (!currentList.any { it.exerciseId == exercise.exerciseId }) {
            currentList.add(exercise)
            _selectedExercises.value = currentList
        }
    }

    fun removeExercise(exerciseId: Long) {
        val currentList = _selectedExercises.value.toMutableList()
        currentList.removeAll { it.exerciseId == exerciseId }
        _selectedExercises.value = currentList
    }

    fun saveTemplate() {
        if (_templateName.value.isBlank()) {
            return
        }

        if (_selectedExercises.value.isEmpty()) {
            return
        }

        viewModelScope.launch {
            _isSaving.value = true
            try {
                // Create the template
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val currentDate = dateFormat.format(Date())

                val template = WorkoutTemplate(
                    date = currentDate,
                    name = _templateName.value,
                    userId = 1L // TODO: Use actual user ID
                )

                val templateId = workoutTemplateDao.insert(template)

                // Add exercises to the template
                val templateExercises = _selectedExercises.value.map { exercise ->
                    TemplateExercise(
                        templateId = templateId,
                        exerciseId = exercise.exerciseId
                    )
                }

                templateExerciseDao.insertAll(templateExercises)

                // Reset state after saving
                _templateName.value = ""
                _selectedExercises.value = emptyList()
            } finally {
                _isSaving.value = false
            }
        }
    }

    companion object {
        fun provideFactory(context: Context): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return CreateTemplateViewModel(context) as T
                }
            }
        }
    }
}

