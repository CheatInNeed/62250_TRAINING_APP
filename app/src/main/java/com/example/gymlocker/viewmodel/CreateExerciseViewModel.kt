package com.example.gymlocker.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.gymlocker.data.database.AppDatabase
import com.example.gymlocker.data.entity.Exercises
import com.example.gymlocker.data.entity.MuscleGroup
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CreateExerciseViewModel(private val appContext: Context) : ViewModel() {

    private val db by lazy { AppDatabase.getDatabase(appContext) }
    private val exerciseDao by lazy { db.exerciseDao() }
    private val muscleGroupDao by lazy { db.muscleGroupDao() }

    // Form state
    private val _exerciseName = MutableStateFlow("")
    val exerciseName: StateFlow<String> = _exerciseName.asStateFlow()

    private val _exerciseNameError = MutableStateFlow<String?>(null)
    val exerciseNameError: StateFlow<String?> = _exerciseNameError.asStateFlow()

    private val _selectedMuscleGroupId = MutableStateFlow<Long?>(null)
    val selectedMuscleGroupId: StateFlow<Long?> = _selectedMuscleGroupId.asStateFlow()

    private val _startWeight = MutableStateFlow(0)
    val startWeight: StateFlow<Int> = _startWeight.asStateFlow()

    private val _startReps = MutableStateFlow(0)
    val startReps: StateFlow<Int> = _startReps.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _saveSuccess = MutableStateFlow(false)
    val saveSuccess: StateFlow<Boolean> = _saveSuccess.asStateFlow()

    // Available muscle groups
    val muscleGroups: StateFlow<List<MuscleGroup>> = muscleGroupDao.getAllMuscleGroups()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateExerciseName(value: String) {
        _exerciseName.value = value
        _exerciseNameError.value = when {
            value.isBlank() -> "Name cannot be empty"
            value.length > MAX_EXERCISE_NAME_LENGTH -> "Max $MAX_EXERCISE_NAME_LENGTH characters"
            else -> null
        }
    }

    fun selectMuscleGroup(muscleGroupId: Long) {
        _selectedMuscleGroupId.value = muscleGroupId
    }

    fun updateStartWeight(value: Int) {
        _startWeight.value = value.coerceAtLeast(0)
    }

    fun updateStartReps(value: Int) {
        _startReps.value = value.coerceAtLeast(0)
    }

    fun canSave(): Boolean {
        return _exerciseName.value.isNotBlank() &&
                _exerciseNameError.value == null &&
                _selectedMuscleGroupId.value != null
    }

    fun saveExercise(onSuccess: () -> Unit) {
        if (!canSave()) return

        viewModelScope.launch {
            _isSaving.value = true
            try {
                val exercise = Exercises(
                    name = _exerciseName.value.trim(),
                    startWeight = _startWeight.value,
                    startReps = _startReps.value,
                    isRecent = true,
                    muscleGroupId = _selectedMuscleGroupId.value!!
                )
                exerciseDao.insert(exercise)
                _saveSuccess.value = true
                onSuccess()
            } catch (e: Exception) {
                _exerciseNameError.value = "Exercise with this name already exists"
            } finally {
                _isSaving.value = false
            }
        }
    }

    fun resetForm() {
        _exerciseName.value = ""
        _exerciseNameError.value = null
        _selectedMuscleGroupId.value = null
        _startWeight.value = 0
        _startReps.value = 0
        _saveSuccess.value = false
    }

    companion object {
        private const val MAX_EXERCISE_NAME_LENGTH = 50

        fun provideFactory(context: Context): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return CreateExerciseViewModel(context.applicationContext) as T
                }
            }
        }
    }
}

