package com.example.gymlocker.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.gymlocker.data.auth.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AuthViewModel(private val repo: AuthRepository) : ViewModel() {

    val isLoggedIn: Flow<Boolean> = repo.isLoggedIn()
    val activeProfileUserId: Flow<Long?> = repo.activeProfileUserId()

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState

    fun register(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val result = repo.register(email, password)
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = result.exceptionOrNull()?.message
            )
        }
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val result = repo.login(email, password)
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = result.exceptionOrNull()?.message
            )
        }
    }

    fun logout() {
        viewModelScope.launch { repo.logout() }
    }

    fun setActiveProfile(profileUserId: Long) {
        viewModelScope.launch { repo.setActiveProfile(profileUserId) }
    }

    fun clearActiveProfile() {
        viewModelScope.launch { repo.clearActiveProfile() }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    data class AuthUiState(
        val isLoading: Boolean = false,
        val error: String? = null
    )

    companion object {
        fun provideFactory(context: Context): ViewModelProvider.Factory {
            val appContext = context.applicationContext
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return AuthViewModel(AuthRepository(appContext)) as T
                }
            }
        }
    }
}
